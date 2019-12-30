package com.ruchij.config

import cats.effect.Sync
import cats.~>
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.clients.CommonClientConfigs
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

sealed trait KafkaClientConfiguration {
  val consumerGroupId: String
}

object KafkaClientConfiguration {
  case class LocalKafkaClientConfiguration(bootstrapServers: String, schemaRegistryUrl: String, consumerGroupId: String)
      extends KafkaClientConfiguration

  def local[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[LocalKafkaClientConfiguration] =
    Sync[F].defer {
      functionK(configObjectSource.at("local-kafka-configuration").load[LocalKafkaClientConfiguration])
    }

  val coreConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(bootstrapServers, _, _) =>
      Map(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers)
  }

  val schemaRegistryConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(_, schemaRegistryUrl, _) =>
      Map(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl)
  }
}

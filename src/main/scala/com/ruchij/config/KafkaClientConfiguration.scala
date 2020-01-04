package com.ruchij.config

import cats.effect.Sync
import cats.~>
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.{SaslConfigs, SslConfigs}
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.security.plain.PlainLoginModule
import org.apache.kafka.common.security.plain.internals.PlainSaslServer
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

sealed trait KafkaClientConfiguration {
  val consumerGroupId: Option[String]
}

object KafkaClientConfiguration {
  case class LocalKafkaClientConfiguration(bootstrapServers: String, schemaRegistryUrl: String, consumerGroupId: Option[String])
      extends KafkaClientConfiguration

  case class ConfluentKafkaClientConfiguration(
    bootstrapServers: String,
    schemaRegistryUrl: String,
    kafkaUsername: String,
    kafkaPassword: String,
    schemaRegistryUsername: String,
    schemaRegistryPassword: String,
    consumerGroupId: Option[String]
  ) extends KafkaClientConfiguration

  def local[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: ConfigReader.Result ~> F): F[LocalKafkaClientConfiguration] =
    Sync[F].defer {
      functionK(configObjectSource.at("local-kafka-configuration").load[LocalKafkaClientConfiguration])
    }

  def confluent[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[ConfluentKafkaClientConfiguration] =
    Sync[F].defer {
      functionK(configObjectSource.at("confluent-kafka-configuration").load[ConfluentKafkaClientConfiguration])
    }

  val coreConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(bootstrapServers, _, _) =>
      Map(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers)

    case confluentKafkaClientConfiguration: ConfluentKafkaClientConfiguration =>
      Map(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> SecurityProtocol.SASL_SSL.name,
        SaslConfigs.SASL_MECHANISM -> PlainSaslServer.PLAIN_MECHANISM,
        SaslConfigs.SASL_JAAS_CONFIG ->
          s"""${classOf[PlainLoginModule].getName} required username="${confluentKafkaClientConfiguration.kafkaUsername}" password="${confluentKafkaClientConfiguration.kafkaPassword}";""",
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG -> SslConfigs.DEFAULT_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> confluentKafkaClientConfiguration.bootstrapServers
      )
  }

  val schemaRegistryConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(_, schemaRegistryUrl, _) =>
      Map(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl)

    case confluentKafkaClientConfiguration: ConfluentKafkaClientConfiguration =>
      Map(
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> confluentKafkaClientConfiguration.schemaRegistryUrl,
        SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE -> "USER_INFO",
        SchemaRegistryClientConfig.USER_INFO_CONFIG -> s"${confluentKafkaClientConfiguration.schemaRegistryUsername}:${confluentKafkaClientConfiguration.schemaRegistryPassword}"
      )
  }
}

package com.ruchij.messaging.kafka.models

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.clients.CommonClientConfigs

sealed trait KafkaClientConfiguration {
  val consumerGroupId: String
}

object KafkaClientConfiguration {
  case class LocalKafkaClientConfiguration(bootstrapServers: String, schemaRegistryUrl: String, consumerGroupId: String)
      extends KafkaClientConfiguration

  val coreConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(bootstrapServers, _, _) =>
      Map(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers)
  }

  val schemaRegistryConfiguration: KafkaClientConfiguration => Map[String, String] = {
    case LocalKafkaClientConfiguration(_, schemaRegistryUrl, _) =>
      Map(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl)
  }
}

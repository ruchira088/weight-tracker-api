package com.ruchij.messaging.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import cats.effect.Async
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.kafka.models.KafkaClientConfiguration
import com.ruchij.messaging.models.Message
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.{Producer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.language.higherKinds

class KafkaProducer[F[_]: Async](kafkaClientConfiguration: KafkaClientConfiguration)(implicit actorSystem: ActorSystem)
    extends Publisher[F, RecordMetadata] {
  lazy val kafkaProducer: Producer[String, AnyRef] =
    KafkaProducer.settings(kafkaClientConfiguration).createKafkaProducer()

  override def publish[A](message: Message[A]): F[RecordMetadata] =
    Async[F].async[RecordMetadata] { callback =>
      kafkaProducer.send(
        new ProducerRecord[String, AnyRef](message.topic.entryName, message.topic.recordFormat.to(message.value)),
        (metadata: RecordMetadata, exception: Exception) =>
          Option(exception).fold(callback(Right(metadata))) {
            _ => callback(Left(exception))
          }
      )
    }
}

object KafkaProducer {
  def settings(
    kafkaClientConfiguration: KafkaClientConfiguration
  )(implicit actorSystem: ActorSystem): ProducerSettings[String, AnyRef] =
    ProducerSettings(
      actorSystem,
      new StringSerializer,
      new KafkaAvroSerializer() {
        configure(KafkaClientConfiguration.schemaRegistryConfiguration(kafkaClientConfiguration).asJava, false)
      }
    ).withProperties {
        KafkaClientConfiguration.coreConfiguration(kafkaClientConfiguration)
      }
}

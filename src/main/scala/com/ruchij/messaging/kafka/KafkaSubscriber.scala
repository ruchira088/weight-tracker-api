package com.ruchij.messaging.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Source
import cats.effect.Sync
import cats.~>
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.KafkaClientConfiguration
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.models.{CommittableMessage, Message, Topic}
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.Future
import scala.language.higherKinds

class KafkaSubscriber[F[_]: Sync](kafkaClientConfiguration: KafkaClientConfiguration)(
  implicit actorSystem: ActorSystem,
  functionK: Future ~> F
) extends Subscriber[F] {
  override type CommitResult = Done
  override type SubscriptionResult = Consumer.Control

  override def subscribe[A](topic: Topic[A]): Source[CommittableMessage[F, A, CommitResult], Consumer.Control] =
    Consumer
      .committableSource(KafkaSubscriber.settings(kafkaClientConfiguration), Subscriptions.topics(topic.entryName))
      .map { committableMessage =>
        committableMessage.record.value() -> committableMessage.committableOffset
      }
      .collect {
        case (indexedRecord: IndexedRecord, commit) =>
          CommittableMessage[F, A, CommitResult](
            Message(topic, topic.recordFormat.from(indexedRecord)),
            Sync[F].defer(functionK(commit.commitScaladsl))
          )
      }
}

object KafkaSubscriber {
  def settings(
    kafkaClientConfiguration: KafkaClientConfiguration
  )(implicit actorSystem: ActorSystem): ConsumerSettings[String, AnyRef] =
    ConsumerSettings[String, AnyRef](
      actorSystem,
      new StringDeserializer,
      new KafkaAvroDeserializer() {
        configure(KafkaClientConfiguration.schemaRegistryConfiguration(kafkaClientConfiguration).asJava, false)
      }
    ).withProperties(KafkaClientConfiguration.coreConfiguration(kafkaClientConfiguration))
      .withGroupId(kafkaClientConfiguration.consumerGroupId)
}

package com.ruchij.services.resource

import java.util.concurrent.ConcurrentHashMap

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.resource.models.Resource

import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter
import scala.language.higherKinds

class InMemoryResourceService[F[_]: Sync](concurrentHashMap: ConcurrentHashMap[String, Resource[F]])
    extends ResourceService[F] {

  override type InsertionResult = Map[String, Resource[F]]

  override type DeletionResult = Resource[F]

  override def insert(key: String, resource: Resource[F]): F[Map[String, Resource[F]]] =
    Sync[F]
      .delay {
        concurrentHashMap.put(key, resource)
      }
      .as(concurrentHashMap.asScala.toMap)

  override def fetch(key: String): OptionT[F, Resource[F]] =
    OptionT {
      Sync[F].delay {
        Option(concurrentHashMap.get(key))
      }
    }

  override def delete(key: String): OptionT[F, Resource[F]] =
    OptionT {
      Sync[F].delay {
        Option(concurrentHashMap.remove(key))
      }
    }
}

object InMemoryResourceService {
  def apply[F[_]: Sync]: InMemoryResourceService[F] =
    new InMemoryResourceService[F](new ConcurrentHashMap[String, Resource[F]]())
}

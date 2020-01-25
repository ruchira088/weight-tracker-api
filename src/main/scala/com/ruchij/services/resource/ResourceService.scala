package com.ruchij.services.resource

import cats.data.OptionT
import com.ruchij.services.resource.models.Resource

import scala.language.higherKinds

trait ResourceService[F[_]] {
  def insert(key: String, resource: Resource[F]): F[String]

  def fetchByKey(key: String): OptionT[F, Resource[F]]
}

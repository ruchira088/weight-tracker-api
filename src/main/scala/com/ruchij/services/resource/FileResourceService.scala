package com.ruchij.services.resource

import cats.data.OptionT
import com.ruchij.services.resource.models.Resource

import scala.language.higherKinds

class FileResourceService[F[_]] extends ResourceService[F] {
  override def insert(key: String, resource: Resource[F]): F[String] = ???

  override def fetchByKey(key: String): OptionT[F, Resource[F]] = ???
}

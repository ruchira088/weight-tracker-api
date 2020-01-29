package com.ruchij.services.resource

import cats.data.OptionT
import com.ruchij.services.resource.models.Resource

import scala.language.higherKinds

trait ResourceService[F[_]] {
  type InsertionResult

  type DeletionResult

  def insert(key: String, resource: Resource[F]): F[InsertionResult]

  def fetch(key: String): OptionT[F, Resource[F]]

  def delete(key: String): OptionT[F, DeletionResult]
}

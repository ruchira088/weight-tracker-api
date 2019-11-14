package com.ruchij.services.health

import com.ruchij.services.health.models.{HealthStatus, ServiceInformation}

import scala.language.higherKinds

trait HealthCheckService[F[_]] {
  def serviceInformation(): F[ServiceInformation]

  def database(): F[HealthStatus]

  def redis(): F[HealthStatus]
}

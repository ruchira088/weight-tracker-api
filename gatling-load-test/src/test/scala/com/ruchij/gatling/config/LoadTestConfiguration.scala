package com.ruchij.gatling.config

import cats.effect.Sync
import cats.~>
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

import scala.language.higherKinds

case class LoadTestConfiguration(baseUrl: String)

object LoadTestConfiguration {
  def load(configObjectSource: ConfigObjectSource): Either[Throwable, LoadTestConfiguration] =
    configObjectSource.load[LoadTestConfiguration].left.map(ConfigReaderException.apply)

  def loadF[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[LoadTestConfiguration] =
    Sync[F].suspend {
      functionK.apply(load(configObjectSource))
    }
}

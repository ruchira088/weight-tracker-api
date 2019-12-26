package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class DoobieConfiguration(driver: String, url: String, user: String, password: String)

object DoobieConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[DoobieConfiguration] =
    Sync[F].suspend {
      functionK(configObjectSource.at("doobie-configuration").load[DoobieConfiguration])
    }
}

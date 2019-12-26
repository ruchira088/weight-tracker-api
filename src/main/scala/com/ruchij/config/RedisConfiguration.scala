package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class RedisConfiguration(host: String, port: Int, password: Option[String])

object RedisConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[RedisConfiguration] =
    Sync[F].suspend {
      functionK(configObjectSource.at("redis-configuration").load[RedisConfiguration])
    }
}

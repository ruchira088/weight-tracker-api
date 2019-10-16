package com.ruchij.config

import cats.data.ReaderT
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import pureconfig.ConfigObjectSource

case class ServiceConfiguration(
  httpConfiguration: HttpConfiguration,
  doobieConfiguration: DoobieConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  redisConfiguration: RedisConfiguration
)

object ServiceConfiguration {
  val load: ReaderT[Either[Exception, *], ConfigObjectSource, ServiceConfiguration] =
    ReaderT {
      _.load[ServiceConfiguration].left.map(ConfigReaderException.apply)
    }
}

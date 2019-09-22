package com.ruchij.config

import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import pureconfig.ConfigSource

case class ServiceConfiguration(
  httpConfiguration: HttpConfiguration,
  doobieConfiguration: DoobieConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  redisConfiguration: RedisConfiguration
)

object ServiceConfiguration {
  def load(): Either[Exception, ServiceConfiguration] =
    ConfigSource.default.load[ServiceConfiguration].left.map(ConfigReaderException.apply)
}

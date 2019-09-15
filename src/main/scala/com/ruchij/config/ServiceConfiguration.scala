package com.ruchij.config

import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import pureconfig.loadConfig

case class ServiceConfiguration(httpConfiguration: HttpConfiguration, doobieConfiguration: DoobieConfiguration)

object ServiceConfiguration {
  def load(): Either[Throwable, ServiceConfiguration] =
    loadConfig[ServiceConfiguration].left.map(ConfigReaderException.apply)
}

package com.ruchij.config

import org.joda.time.DateTime
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import pureconfig.{ConfigObjectSource, ConfigReader}

import scala.util.Try

case class ServiceConfiguration(
  httpConfiguration: HttpConfiguration,
  doobieConfiguration: DoobieConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  redisConfiguration: RedisConfiguration,
  emailConfiguration: EmailConfiguration,
  developmentConfiguration: DevelopmentConfiguration,
  buildInformation: BuildInformation
)

object ServiceConfiguration {
  implicit val dateTimeReader: ConfigReader[DateTime] =
    ConfigReader.fromNonEmptyStringTry[DateTime](string => Try(DateTime.parse(string)))

  def load(configObjectSource: ConfigObjectSource): Either[Throwable, ServiceConfiguration] =
    configObjectSource.load[ServiceConfiguration].left.map(ConfigReaderException.apply)
}

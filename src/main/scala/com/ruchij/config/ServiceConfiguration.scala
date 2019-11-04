package com.ruchij.config

import cats.data.ReaderT
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
  buildInformation: BuildInformation
)

object ServiceConfiguration {
  implicit val dateTimeReader: ConfigReader[DateTime] =
    ConfigReader.fromNonEmptyStringTry[DateTime](string => Try(DateTime.parse(string)))

  val load: ReaderT[Either[Exception, *], ConfigObjectSource, ServiceConfiguration] =
    ReaderT {
      _.load[ServiceConfiguration].left.map(ConfigReaderException.apply)
    }
}

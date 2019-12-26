package com.ruchij.config

import cats.effect.Sync
import cats.~>
import com.ruchij.config.development.ApplicationMode
import org.joda.time.DateTime
import pureconfig.generic.auto._
import pureconfig.{ConfigObjectSource, ConfigReader}

import scala.language.higherKinds
import scala.util.Try

case class ServiceConfiguration(
  httpConfiguration: HttpConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  applicationMode: ApplicationMode,
  buildInformation: BuildInformation
)

object ServiceConfiguration {
  implicit val dateTimeReader: ConfigReader[DateTime] =
    ConfigReader.fromNonEmptyStringTry[DateTime](string => Try(DateTime.parse(string)))

  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[ServiceConfiguration] =
    Sync[F].suspend {
      functionK(configObjectSource.load[ServiceConfiguration])
    }
}

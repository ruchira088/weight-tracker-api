package com.ruchij.email.config

import cats.effect.Sync
import cats.~>
import com.ruchij.config.development.ApplicationMode
import pureconfig.{ConfigObjectSource, ConfigReader}

import scala.language.higherKinds

case class EmailServiceConfiguration(applicationMode: ApplicationMode)

object EmailServiceConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[EmailServiceConfiguration] =
    Sync[F].defer {
      functionK(configObjectSource.load[EmailServiceConfiguration])
    }
}

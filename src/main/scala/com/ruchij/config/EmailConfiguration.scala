package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class EmailConfiguration(sendgridApiKey: String)

object EmailConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[EmailConfiguration] =
    Sync[F].suspend {
      functionK(configObjectSource.at("email-configuration").load[EmailConfiguration])
    }
}

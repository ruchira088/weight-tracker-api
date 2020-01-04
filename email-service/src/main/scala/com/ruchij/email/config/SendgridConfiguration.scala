package com.ruchij.email.config

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class SendgridConfiguration(sendgridApiKey: String)

object SendgridConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[SendgridConfiguration] =
    Sync[F].suspend {
      functionK(configObjectSource.at("sendgrid-configuration").load[SendgridConfiguration])
    }
}

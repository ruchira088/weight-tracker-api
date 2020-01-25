package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class S3Configuration(bucket: String, prefixKey: String)

object S3Configuration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[S3Configuration] =
    Sync[F].defer {
      functionK {
        configObjectSource.at("s3-configuration").load[S3Configuration]
      }
    }
}

package com.ruchij.config

import java.nio.file.Path

import cats.effect.Sync
import cats.~>
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.generic.auto._

import scala.language.higherKinds

case class FileResourceConfiguration(fileResourceFolder: Path, metaDataFile: Path)

object FileResourceConfiguration {
  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: ConfigReader.Result ~> F): F[FileResourceConfiguration] =
    Sync[F].defer {
      functionK {
        configObjectSource.at("file-resource-configuration").load[FileResourceConfiguration]
      }
    }
}

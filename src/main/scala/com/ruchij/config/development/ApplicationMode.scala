package com.ruchij.config.development

import com.ruchij.circe.CirceEnum
import enumeratum.{Enum, EnumEntry}
import pureconfig.ConfigReader
import pureconfig.error.{FailureReason, KeyNotFound}

import scala.collection.immutable.IndexedSeq

sealed trait ApplicationMode extends EnumEntry

object ApplicationMode extends Enum[ApplicationMode] with CirceEnum[ApplicationMode] {
  case object Production extends ApplicationMode
  case object DockerCompose extends ApplicationMode
  case object Local extends ApplicationMode
  case object Test extends ApplicationMode

  override def values: IndexedSeq[ApplicationMode] = findValues

  implicit val applicationModeConfigReader: ConfigReader[ApplicationMode] =
    ConfigReader.fromNonEmptyString[ApplicationMode] { value =>
      withNameInsensitiveOption(value)
        .fold[Either[FailureReason, ApplicationMode]](Left(KeyNotFound(value, values.map(_.entryName).toSet)))(Right.apply)
    }
}

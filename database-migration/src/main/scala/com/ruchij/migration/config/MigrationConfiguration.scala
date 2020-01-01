package com.ruchij.migration.config

import pureconfig.error.ConfigReaderException
import pureconfig.ConfigObjectSource
import pureconfig.generic.auto._

case class MigrationConfiguration(databaseConfiguration: DatabaseConfiguration)

object MigrationConfiguration {
  def load(configObjectSource: ConfigObjectSource): Either[Exception, MigrationConfiguration] =
    configObjectSource.load[MigrationConfiguration].left.map(ConfigReaderException.apply)
}

package com.ruchij.migration.config

import pureconfig.error.ConfigReaderException
import pureconfig.loadConfig
import pureconfig.generic.auto._

case class MigrationConfiguration(databaseConfiguration: DatabaseConfiguration)

object MigrationConfiguration {
  def load(): Either[Exception, MigrationConfiguration] =
    loadConfig[MigrationConfiguration].left.map(ConfigReaderException.apply)
}

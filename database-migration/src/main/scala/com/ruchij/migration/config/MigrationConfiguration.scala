package com.ruchij.migration.config

import pureconfig.error.ConfigReaderException
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class MigrationConfiguration(databaseConfiguration: DatabaseConfiguration)

object MigrationConfiguration {
  def load(): Either[Exception, MigrationConfiguration] =
    ConfigSource.default.load[MigrationConfiguration].left.map(ConfigReaderException.apply)
}

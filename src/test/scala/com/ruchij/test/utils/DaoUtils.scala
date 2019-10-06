package com.ruchij.test.utils

import cats.effect.{Async, ContextShift}
import com.ruchij.migration.config.DatabaseConfiguration
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

import scala.language.higherKinds

object DaoUtils {

  val H2_DATABASE_CONFIGURATION =
    DatabaseConfiguration(
      "jdbc:h2:mem:weight-tracker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "",
      ""
    )

  def h2Transactor[F[_]: Async: ContextShift]: Aux[F, Unit] =
    Transactor.fromDriverManager("org.h2.Driver", H2_DATABASE_CONFIGURATION.url)
}

package com.ruchij.daos.doobie

import cats.effect.{Async, ContextShift}
import com.ruchij.config.DoobieConfiguration
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

import scala.language.higherKinds

object DoobieTransactor {
  def fromConfiguration[F[_] : Async : ContextShift](doobieConfiguration: DoobieConfiguration): Aux[F, Unit] =
    Transactor.fromDriverManager[F](
      doobieConfiguration.driver,
      doobieConfiguration.url,
      doobieConfiguration.user,
      doobieConfiguration.password
    )
}

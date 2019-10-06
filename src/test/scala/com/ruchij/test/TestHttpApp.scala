package com.ruchij.test

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import cats.data.ValidatedNel
import cats.effect.{Async, Clock, ContextShift, Sync}
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.daos.weightentry.DoobieWeightEntryDao
import com.ruchij.migration.MigrationApp
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.test.daos.InMemoryAuthenticationTokenDao
import com.ruchij.test.utils.DaoUtils
import com.ruchij.types.Transformation.~>
import com.ruchij.types.{Random, Transformation}
import com.ruchij.web.Routes
import org.http4s.HttpApp

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

object TestHttpApp {
  def apply[F[_]: Async: ContextShift: Clock: Lambda[X[_] => Random[X, UUID]]: Lambda[
    X[_] => ValidatedNel[Throwable, *] ~> X
  ]](): HttpApp[F] = {
    MigrationApp.migrate(DaoUtils.H2_DATABASE_CONFIGURATION).unsafeRunSync()

    val userDao: DoobieUserDao[F] = new DoobieUserDao[F](DaoUtils.h2Transactor)
    val weightEntryDao: DoobieWeightEntryDao[F] = new DoobieWeightEntryDao[F](DaoUtils.h2Transactor)

    val authenticationService =
      new AuthenticationServiceImpl[F](
        new BCryptService[F](ExecutionContext.global),
        userDao,
        new InMemoryAuthenticationTokenDao[F](new ConcurrentHashMap[String, AuthenticationToken]()),
        new AuthenticationSecretGeneratorImpl[F],
        AuthenticationConfiguration(30 seconds)
      )

    val userService = new UserServiceImpl[F](userDao, authenticationService)
    val weightEntryService = new WeightEntryServiceImpl[F](weightEntryDao)
    val healthCheckService = new HealthCheckServiceImpl[F]

    Routes.responseHandler {
      Routes[F](userService, weightEntryService, healthCheckService)(
        Sync[F],
        authenticationService,
        new AuthorizationServiceImpl[F],
        Transformation[ValidatedNel[Throwable, *], F]
      )
    }
  }
}

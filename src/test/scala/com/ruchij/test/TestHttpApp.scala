package com.ruchij.test

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import cats.Functor
import cats.data.ValidatedNel
import cats.effect.{Async, Clock, ContextShift, Sync}
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.daos.weightentry.DoobieWeightEntryDao
import com.ruchij.migration.MigrationApp
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.authentication.{
  AuthenticationSecretGeneratorImpl,
  AuthenticationService,
  AuthenticationServiceImpl
}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.{WeightEntryService, WeightEntryServiceImpl}
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.{UserService, UserServiceImpl}
import com.ruchij.test.daos.InMemoryAuthenticationTokenDao
import com.ruchij.test.utils.DaoUtils
import com.ruchij.types.Transformation.~>
import com.ruchij.types.{Random, Transformation, UnsafeCopoint}
import com.ruchij.web.Routes
import org.http4s.HttpApp

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

case class TestHttpApp[F[_]](
  httpApp: HttpApp[F],
  userService: UserService[F],
  authenticationService: AuthenticationService[F],
  weightEntryService: WeightEntryService[F]
)

object TestHttpApp {
  def apply[F[_]: Async: ContextShift: Clock: Lambda[X[_] => Random[X, UUID]]: Lambda[
    X[_] => ValidatedNel[Throwable, *] ~> X
  ]](): TestHttpApp[F] = {
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

    val httpApp =
      Routes.responseHandler {
        Routes[F](userService, weightEntryService, healthCheckService)(
          Sync[F],
          authenticationService,
          new AuthorizationServiceImpl[F],
          Transformation[ValidatedNel[Throwable, *], F]
        )
      }

    TestHttpApp(httpApp, userService, authenticationService, weightEntryService)
  }

  implicit class TestHttpAppOps[F[_]: UnsafeCopoint](val testHttpApp: TestHttpApp[F]) {
    def withUser(databaseUser: DatabaseUser): TestHttpApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          testHttpApp.userService.create(
            databaseUser.username,
            databaseUser.password,
            databaseUser.email,
            databaseUser.firstName,
            databaseUser.lastName
          )
        }
      }

    def self(block: Unit): TestHttpApp[F] = testHttpApp
  }
}

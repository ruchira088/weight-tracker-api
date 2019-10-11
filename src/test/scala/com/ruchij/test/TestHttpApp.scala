package com.ruchij.test

import java.util.UUID

import akka.actor.ActorSystem
import cats.data.ValidatedNel
import cats.effect.{Async, Clock, ContextShift, Sync}
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.authtokens.{AuthenticationTokenDao, RedisAuthenticationTokenDao}
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.daos.user.{DoobieUserDao, UserDao}
import com.ruchij.daos.weightentry.{DoobieWeightEntryDao, WeightEntryDao}
import com.ruchij.migration.MigrationApp
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.test.utils.{DaoUtils, RandomGenerator}
import com.ruchij.types.Transformation.~>
import com.ruchij.types.{Random, Transformation, UnsafeCopoint}
import com.ruchij.web.Routes
import org.http4s.HttpApp
import redis.RedisClient
import redis.embedded.RedisServer
import redis.embedded.ports.EphemeralPortProvider

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

case class TestHttpApp[F[_]](
  httpApp: HttpApp[F],
  userDao: UserDao[F],
  authenticationTokenDao: AuthenticationTokenDao[F],
  weightEntryDao: WeightEntryDao[F],
  shutdownHook: () => Unit
)

object TestHttpApp {

  def apply[F[_]: Async: ContextShift: Clock: Lambda[X[_] => Random[X, UUID]]: Lambda[
    X[_] => ValidatedNel[Throwable, *] ~> X
  ]: Lambda[X[_] => Future ~> X]](): TestHttpApp[F] = {

    MigrationApp.migrate(DaoUtils.H2_DATABASE_CONFIGURATION).unsafeRunSync()

    val userDao: DoobieUserDao[F] = new DoobieUserDao[F](DaoUtils.h2Transactor)
    val weightEntryDao: DoobieWeightEntryDao[F] = new DoobieWeightEntryDao[F](DaoUtils.h2Transactor)

    //    val authenticationTokenDao: InMemoryAuthenticationTokenDao[F] =
//      new InMemoryAuthenticationTokenDao[F](new ConcurrentHashMap[String, AuthenticationToken]())

    val redisPort = new EphemeralPortProvider().next()
    val redisServer = new RedisServer(redisPort)

    redisServer.start()

    implicit val actorSystem: ActorSystem = ActorSystem(s"redis-${RandomGenerator.uuid()}")

    val authenticationTokenDao = new RedisAuthenticationTokenDao[F](RedisClient(port = redisPort))

    val authenticationService =
      new AuthenticationServiceImpl[F](
        new BCryptService[F](ExecutionContext.global),
        userDao,
        authenticationTokenDao,
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

    val shutdownHook: () => Unit = () => {
      Await.ready(actorSystem.terminate(), 5 seconds)
      redisServer.stop()
    }

    TestHttpApp(httpApp, userDao, authenticationTokenDao, weightEntryDao, shutdownHook)
  }

  implicit class TestHttpAppOps[F[_]: UnsafeCopoint](val testHttpApp: TestHttpApp[F]) {
    def withUser(databaseUser: DatabaseUser): TestHttpApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          testHttpApp.userDao.insert(databaseUser)
        }
      }

    def withAuthenticationToken(authenticationToken: AuthenticationToken): TestHttpApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          testHttpApp.authenticationTokenDao.createToken(authenticationToken)
        }
      }

    def self(block: Unit): TestHttpApp[F] = testHttpApp

    def shutdown(): Unit = testHttpApp.shutdownHook()
  }
}

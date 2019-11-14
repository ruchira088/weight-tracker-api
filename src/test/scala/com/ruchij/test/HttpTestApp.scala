package com.ruchij.test

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.ActorSystem
import cats.{Applicative, ~>}
import cats.data.ValidatedNel
import cats.effect.{Async, Clock, ContextShift}
import cats.implicits._
import com.ruchij.config.{AuthenticationConfiguration, BuildInformation, RedisConfiguration}
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.authtokens.{AuthenticationTokenDao, RedisAuthenticationTokenDao}
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.daos.resetpassword.{DoobieResetPasswordTokenDao, ResetPasswordTokenDao}
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.daos.user.{DoobieUserDao, UserDao}
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.daos.weightentry.{DoobieWeightEntryDao, WeightEntryDao}
import com.ruchij.migration.MigrationApp
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.email.models.Email
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.test.stubs.StubbedEmailService
import com.ruchij.test.utils.DaoUtils
import com.ruchij.types.{Random, UnsafeCopoint}
import com.ruchij.web.Routes
import org.http4s.HttpApp
import redis.embedded.RedisServer
import redis.embedded.ports.EphemeralPortProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.{higherKinds, postfixOps}

case class HttpTestApp[F[_]](
  httpApp: HttpApp[F],
  userDao: UserDao[F],
  authenticationTokenDao: AuthenticationTokenDao[F],
  weightEntryDao: WeightEntryDao[F],
  resetPasswordTokenDao: ResetPasswordTokenDao[F],
  externalEmailMailBox: ConcurrentLinkedQueue[Email],
  shutdownHook: () => Unit
)

object HttpTestApp {

  def apply[F[_]: Async: ContextShift: Clock: Lambda[X[_] => Random[X, UUID]]: Lambda[
    X[_] => ValidatedNel[Throwable, *] ~> X
  ]: Lambda[X[_] => Future ~> X]](): HttpTestApp[F] = {

    MigrationApp.migrate(DaoUtils.H2_DATABASE_CONFIGURATION).unsafeRunSync()

    val userDao: DoobieUserDao[F] = new DoobieUserDao[F](DaoUtils.h2Transactor)
    val resetPasswordTokenDao: DoobieResetPasswordTokenDao[F] =
      new DoobieResetPasswordTokenDao[F](DaoUtils.h2Transactor)
    val weightEntryDao: DoobieWeightEntryDao[F] = new DoobieWeightEntryDao[F](DaoUtils.h2Transactor)

    //    val authenticationTokenDao: InMemoryAuthenticationTokenDao[F] =
//      new InMemoryAuthenticationTokenDao[F](new ConcurrentHashMap[String, AuthenticationToken]())

    val redisPort = new EphemeralPortProvider().next()
    val redisServer = new RedisServer(redisPort)

    redisServer.start()

    val buildInformation = BuildInformation(Some("master"), Some("abc1234"), None)

    implicit val actorSystem: ActorSystem = ActorSystem("redis-actor-system")

    val redisClient = RedisAuthenticationTokenDao.redisClient(RedisConfiguration("localhost", redisPort, None))

    val authenticationTokenDao = new RedisAuthenticationTokenDao[F](redisClient)

    val emailMailBox = new ConcurrentLinkedQueue[Email]()
    val stubbedEmailService = new StubbedEmailService[F](emailMailBox)

    val authenticationService =
      new AuthenticationServiceImpl[F](
        new BCryptService[F](ExecutionContext.global),
        stubbedEmailService,
        userDao,
        resetPasswordTokenDao,
        authenticationTokenDao,
        new AuthenticationSecretGeneratorImpl[F],
        AuthenticationConfiguration(30 seconds)
      )

    val userService = new UserServiceImpl[F](userDao, authenticationService, stubbedEmailService)
    val weightEntryService = new WeightEntryServiceImpl[F](weightEntryDao)
    val healthCheckService = new HealthCheckServiceImpl[F](DaoUtils.h2Transactor, redisClient, buildInformation)
    val authorizationService = new AuthorizationServiceImpl[F]

    val httpApp =
      Routes[F](userService, weightEntryService, healthCheckService, authenticationService, authorizationService)

    val shutdownHook: () => Unit = () => {
      Await.ready(actorSystem.terminate(), 5 seconds)
      redisServer.stop()
    }

    HttpTestApp(
      httpApp,
      userDao,
      authenticationTokenDao,
      weightEntryDao,
      resetPasswordTokenDao,
      emailMailBox,
      shutdownHook
    )
  }

  implicit class HttpTestAppOps[F[_]: UnsafeCopoint: Applicative](val httpTestApp: HttpTestApp[F]) {
    def withUser(databaseUser: DatabaseUser): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          httpTestApp.userDao.insert(databaseUser)
        }
      }

    def withAuthenticationToken(databaseAuthenticationToken: DatabaseAuthenticationToken): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          httpTestApp.authenticationTokenDao.createToken(databaseAuthenticationToken)
        }
      }

    def withWeightEntries(databaseWeightEntries: DatabaseWeightEntry*): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          databaseWeightEntries.toList.traverse(httpTestApp.weightEntryDao.insert)
        }
      }

    def withResetPasswordToken(databaseResetPasswordToken: DatabaseResetPasswordToken): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          httpTestApp.resetPasswordTokenDao.insert(databaseResetPasswordToken)
        }
      }

    def self(block: Unit): HttpTestApp[F] = httpTestApp

    def shutdown(): Unit = httpTestApp.shutdownHook()
  }
}

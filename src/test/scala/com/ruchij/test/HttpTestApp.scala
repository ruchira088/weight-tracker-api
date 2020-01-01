package com.ruchij.test

import java.util.UUID

import akka.actor.ActorSystem
import cats.data.ValidatedNel
import cats.effect.{Async, Clock, ContextShift}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.AuthenticationConfiguration.BruteForceProtectionConfiguration
import com.ruchij.config.development.{ApplicationMode, ExternalComponents}
import com.ruchij.config.{AuthenticationConfiguration, BuildInformation}
import com.ruchij.daos.authenticationfailure.DoobieAuthenticationFailureDao
import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.authtokens.{AuthenticationTokenDao, RedisAuthenticationTokenDao}
import com.ruchij.daos.lockeduser.DoobieLockedUserDao
import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.daos.resetpassword.{DoobieResetPasswordTokenDao, ResetPasswordTokenDao}
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.daos.user.{DoobieUserDao, UserDao}
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.daos.weightentry.{DoobieWeightEntryDao, WeightEntryDao}
import com.ruchij.messaging.inmemory.InMemoryPublisher
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.types.{Random, UnsafeCopoint}
import com.ruchij.web.Routes
import org.http4s.HttpApp

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
  authenticationFailureDao: DoobieAuthenticationFailureDao[F],
  lockedUserDao: DoobieLockedUserDao[F],
  inMemoryPublisher: InMemoryPublisher[F],
  shutdownHook: () => Unit
)

object HttpTestApp {
  val SESSION_TIMEOUT: FiniteDuration = 30 seconds

  def apply[F[_]: Async: ContextShift: Clock: Random[*[_], UUID]: ValidatedNel[Throwable, *] ~> *[_]: Future ~> *[_]: UnsafeCopoint]()
    : HttpTestApp[F] = {

    implicit val actorSystem: ActorSystem = ActorSystem("redis-actor-system")

    val externalComponents: ExternalComponents[F] = UnsafeCopoint.unsafeExtract(ExternalComponents.local[F, F]())

    val userDao: DoobieUserDao[F] = new DoobieUserDao[F](externalComponents.transactor)
    val resetPasswordTokenDao: DoobieResetPasswordTokenDao[F] =
      new DoobieResetPasswordTokenDao[F](externalComponents.transactor)
    val weightEntryDao: DoobieWeightEntryDao[F] = new DoobieWeightEntryDao[F](externalComponents.transactor)
    val lockedUserDao = new DoobieLockedUserDao[F](externalComponents.transactor)
    val authenticationFailureDao = new DoobieAuthenticationFailureDao[F](externalComponents.transactor)

    val buildInformation = BuildInformation(Some("master"), Some("abc1234"), None)

    val authenticationTokenDao = new RedisAuthenticationTokenDao[F](externalComponents.redisClient)

    val inMemoryPublisher = InMemoryPublisher.empty[F]

    val authenticationService =
      new AuthenticationServiceImpl[F](
        new BCryptService[F](ExecutionContext.global),
        inMemoryPublisher,
        userDao,
        lockedUserDao,
        authenticationFailureDao,
        resetPasswordTokenDao,
        authenticationTokenDao,
        new AuthenticationSecretGeneratorImpl[F],
        AuthenticationConfiguration(SESSION_TIMEOUT, BruteForceProtectionConfiguration(3, 30 seconds))
      )

    val userService =
      new UserServiceImpl[F](userDao, lockedUserDao, authenticationService, inMemoryPublisher)
    val weightEntryService = new WeightEntryServiceImpl[F](weightEntryDao)
    val healthCheckService = new HealthCheckServiceImpl[F](
      externalComponents.transactor,
      externalComponents.redisClient,
      ApplicationMode.Local,
      buildInformation
    )
    val authorizationService = new AuthorizationServiceImpl[F]

    val httpApp =
      Routes[F](userService, weightEntryService, healthCheckService, authenticationService, authorizationService)

    val shutdownHook: () => Unit = () => {
      Await.ready(actorSystem.terminate(), 5 seconds)
      UnsafeCopoint.unsafeExtract(externalComponents.shutdownHook())
    }

    HttpTestApp(
      httpApp,
      userDao,
      authenticationTokenDao,
      weightEntryDao,
      resetPasswordTokenDao,
      authenticationFailureDao,
      lockedUserDao,
      inMemoryPublisher,
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

    def withAuthenticationFailure(databaseAuthenticationFailure: DatabaseAuthenticationFailure): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          httpTestApp.authenticationFailureDao.insert(databaseAuthenticationFailure)
        }
      }

    def withLockedUser(databaseLockedUser: DatabaseLockedUser): HttpTestApp[F] =
      self {
        UnsafeCopoint.unsafeExtract {
          httpTestApp.lockedUserDao.insert(databaseLockedUser)
        }
      }

    def self(block: Unit): HttpTestApp[F] = httpTestApp

    def shutdown(): Unit = httpTestApp.shutdownHook()
  }
}

package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.authenticationfailure.DoobieAuthenticationFailureDao
import com.ruchij.daos.authtokens.RedisAuthenticationTokenDao
import com.ruchij.daos.doobie.DoobieTransactor
import com.ruchij.daos.lockeduser.DoobieLockedUserDao
import com.ruchij.daos.resetpassword.DoobieResetPasswordTokenDao
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.daos.weightentry.DoobieWeightEntryDao
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.Routes
import com.sendgrid.SendGrid
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object App extends IOApp {
  implicit lazy val actorSystem: ActorSystem = ActorSystem(BuildInfo.name)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      serviceConfiguration <- IO.suspend(IO.fromEither(ServiceConfiguration.load(ConfigSource.default)))

      cpuBlockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool())
      ioBlockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

      redisClient = RedisAuthenticationTokenDao.redisClient(serviceConfiguration.redisConfiguration)

      doobieTransactor = DoobieTransactor.fromConfiguration[IO](serviceConfiguration.doobieConfiguration)
      databaseUserDao = new DoobieUserDao(doobieTransactor)
      resetPasswordTokenDao = new DoobieResetPasswordTokenDao(doobieTransactor)
      weightEntryDao = new DoobieWeightEntryDao(doobieTransactor)
      lockedUserDao = new DoobieLockedUserDao(doobieTransactor)
      authenticationFailuresDao = new DoobieAuthenticationFailureDao(doobieTransactor)

      passwordHashingService = new BCryptService[IO](cpuBlockingExecutionContext)
      authenticationTokenDao = new RedisAuthenticationTokenDao[IO](redisClient)
      authenticationSecretGenerator = new AuthenticationSecretGeneratorImpl[IO]
      emailService = new SendGridEmailService[IO](
        new SendGrid(serviceConfiguration.emailConfiguration.sendgridApiKey),
        ioBlockingExecutionContext
      )

      healthCheckService = new HealthCheckServiceImpl[IO](doobieTransactor, redisClient, serviceConfiguration.buildInformation)

      authenticationService = new AuthenticationServiceImpl(
        passwordHashingService,
        emailService,
        databaseUserDao,
        lockedUserDao,
        authenticationFailuresDao,
        resetPasswordTokenDao,
        authenticationTokenDao,
        authenticationSecretGenerator,
        serviceConfiguration.authenticationConfiguration
      )

      authorizationService = new AuthorizationServiceImpl[IO]

      userService = new UserServiceImpl(databaseUserDao, authenticationService, emailService)
      weightEntryService = new WeightEntryServiceImpl(weightEntryDao)

      exitCode <- BlazeServerBuilder[IO]
        .withHttpApp {
          Routes(userService, weightEntryService, healthCheckService, authenticationService, authorizationService)
        }
        .bindHttp(serviceConfiguration.httpConfiguration.port, "0.0.0.0")
        .withoutBanner
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
}

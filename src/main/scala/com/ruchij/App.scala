package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.ApiServiceConfiguration
import com.ruchij.config.development.ExternalComponents
import com.ruchij.daos.authenticationfailure.DoobieAuthenticationFailureDao
import com.ruchij.daos.authtokens.RedisAuthenticationTokenDao
import com.ruchij.daos.lockeduser.DoobieLockedUserDao
import com.ruchij.daos.resetpassword.DoobieResetPasswordTokenDao
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.daos.weightentry.DoobieWeightEntryDao
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.Routes
import com.ruchij.web.assets.StaticResourceService
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.{ConfigObjectSource, ConfigSource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object App extends IOApp {

  def application(
    serviceConfiguration: ApiServiceConfiguration,
    configObjectSource: ConfigObjectSource
  ): IO[HttpApp[IO]] = {
    implicit val actorSystem: ActorSystem = ActorSystem(BuildInfo.name)

    val cpuBlockingExecutionContext: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newWorkStealingPool())

    val ioBlockingExecutionContext: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

    ExternalComponents
      .from[IO, IO](serviceConfiguration.applicationMode, configObjectSource)
      .map { externalComponents =>
        val databaseUserDao = new DoobieUserDao(externalComponents.transactor)
        val resetPasswordTokenDao = new DoobieResetPasswordTokenDao(externalComponents.transactor)
        val weightEntryDao = new DoobieWeightEntryDao(externalComponents.transactor)
        val lockedUserDao = new DoobieLockedUserDao(externalComponents.transactor)
        val authenticationFailuresDao = new DoobieAuthenticationFailureDao(externalComponents.transactor)

        val passwordHashingService = new BCryptService[IO](Blocker.liftExecutionContext(cpuBlockingExecutionContext))
        val authenticationTokenDao = new RedisAuthenticationTokenDao[IO](externalComponents.redisClient)
        val authenticationSecretGenerator = new AuthenticationSecretGeneratorImpl[IO]

        val staticResourceService =
          new StaticResourceService[IO](Blocker.liftExecutionContext(ioBlockingExecutionContext))

        val healthCheckService = new HealthCheckServiceImpl[IO](
          externalComponents.transactor,
          externalComponents.redisClient,
          externalComponents.publisher,
          serviceConfiguration.applicationMode,
          serviceConfiguration.buildInformation
        )

        val authenticationService = new AuthenticationServiceImpl(
          passwordHashingService,
          externalComponents.publisher,
          databaseUserDao,
          lockedUserDao,
          authenticationFailuresDao,
          resetPasswordTokenDao,
          authenticationTokenDao,
          authenticationSecretGenerator,
          serviceConfiguration.authenticationConfiguration
        )

        val authorizationService = new AuthorizationServiceImpl[IO]

        val userService =
          new UserServiceImpl(
            databaseUserDao,
            lockedUserDao,
            authenticationService,
            externalComponents.publisher,
            externalComponents.resourceService
          )
        val weightEntryService = new WeightEntryServiceImpl(weightEntryDao)

        Runtime.getRuntime.addShutdownHook {
          new Thread(() => externalComponents.shutdownHook.unsafeRunSync())
        }

        Routes(
          userService,
          weightEntryService,
          healthCheckService,
          authenticationService,
          authorizationService,
          externalComponents.resourceService,
          staticResourceService
        )
      }
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ApiServiceConfiguration.load[IO](configObjectSource)

      httpApp <- application(serviceConfiguration, configObjectSource)

      exitCode <- BlazeServerBuilder[IO]
        .withHttpApp(httpApp)
        .bindHttp(serviceConfiguration.httpConfiguration.port, "0.0.0.0")
        .withoutBanner
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
}

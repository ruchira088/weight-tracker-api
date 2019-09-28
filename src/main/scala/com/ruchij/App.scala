package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.authtokens.RedisAuthenticationTokenDao
import com.ruchij.daos.doobie.DoobieTransactor
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.daos.weightentry.DoobieWeightEntryDao
import com.ruchij.services.authentication.{AuthenticationSecretGeneratorImpl, AuthenticationServiceImpl}
import com.ruchij.services.authorization.AuthorizationServiceImpl
import com.ruchij.services.data.WeightEntryServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.web.Routes
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object App extends IOApp {
  implicit lazy val actorSystem: ActorSystem = ActorSystem(BuildInfo.name)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      serviceConfiguration <- IO.fromEither(ServiceConfiguration.load())

      systemCoreCount <- IO(Runtime.getRuntime.availableProcessors())

      cpuBlockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(systemCoreCount))

      redisClient = RedisAuthenticationTokenDao.redisClient(serviceConfiguration.redisConfiguration)

      doobieTransactor = DoobieTransactor.fromConfiguration[IO](serviceConfiguration.doobieConfiguration)
      databaseUserDao = new DoobieUserDao(doobieTransactor)
      weightEntryDao = new DoobieWeightEntryDao(doobieTransactor)

      passwordHashingService = new BCryptService[IO](cpuBlockingExecutionContext)
      authenticationTokenDao = new RedisAuthenticationTokenDao[IO](redisClient)
      authenticationSecretGenerator = new AuthenticationSecretGeneratorImpl[IO]

      healthCheckService = new HealthCheckServiceImpl[IO]

      authenticationService = new AuthenticationServiceImpl(
        passwordHashingService,
        databaseUserDao,
        authenticationTokenDao,
        authenticationSecretGenerator,
        serviceConfiguration.authenticationConfiguration
      )

      authorizationService = new AuthorizationServiceImpl[IO]

      userService = new UserServiceImpl(databaseUserDao, authenticationService)
      weightEntryService = new WeightEntryServiceImpl(weightEntryDao)

      exitCode <- BlazeServerBuilder[IO]
        .withHttpApp {
          Routes.responseHandler {
            Routes(userService, weightEntryService, healthCheckService)(Sync[IO], authenticationService, authorizationService)
          }
        }
        .bindHttp(serviceConfiguration.httpConfiguration.port)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
}

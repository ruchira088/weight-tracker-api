package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.authtokens.RedisAuthenticationTokenDao
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.services.authentication.AuthenticationServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.web.Routes
import org.http4s.implicits._
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

      healthCheckService = new HealthCheckServiceImpl[IO]
      databaseUserDao = DoobieUserDao.fromConfiguration[IO](serviceConfiguration.doobieConfiguration)
      passwordHashingService = new BCryptService[IO](cpuBlockingExecutionContext)
      authenticationTokenDao = new RedisAuthenticationTokenDao[IO](redisClient)
      authenticationService = new AuthenticationServiceImpl(
        passwordHashingService,
        databaseUserDao,
        authenticationTokenDao,
        serviceConfiguration.authenticationConfiguration
      )
      userService = new UserServiceImpl(databaseUserDao, authenticationService)

      exitCode <- BlazeServerBuilder[IO]
        .withHttpApp(Routes.responseHandler(Routes(userService, healthCheckService)))
        .bindHttp(serviceConfiguration.httpConfiguration.port)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
}

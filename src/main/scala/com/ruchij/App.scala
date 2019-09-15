package com.ruchij

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.user.DoobieDatabaseUserDao
import com.ruchij.services.authentication.AuthenticationServiceImpl
import com.ruchij.services.hashing.BCryptService
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.web.Routes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      serviceConfiguration <- IO.fromEither(ServiceConfiguration.load())

      systemCoreCount <- IO(Runtime.getRuntime.availableProcessors())

      cpuBlockingExecutionContext =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(systemCoreCount))

      healthCheckService = new HealthCheckServiceImpl[IO]
      databaseUserDao = DoobieDatabaseUserDao.fromConfiguration[IO](serviceConfiguration.doobieConfiguration)
      passwordHashingService = new BCryptService[IO](cpuBlockingExecutionContext)
      authenticationService = new AuthenticationServiceImpl(passwordHashingService)
      userService = new UserServiceImpl(databaseUserDao, authenticationService)

      exitCode <-
        BlazeServerBuilder[IO]
          .withHttpApp(Routes(userService, healthCheckService).orNotFound)
          .bindHttp(serviceConfiguration.httpConfiguration.port)
          .serve.compile.drain.as(ExitCode.Success)
    }
    yield exitCode
}

package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.ApiServiceConfiguration
import com.ruchij.config.development.ExternalComponents
import com.ruchij.daos.authentication.DoobieUserAuthenticationConfigurationDao
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

object App extends IOApp {

  def application(
    serviceConfiguration: ApiServiceConfiguration,
    configObjectSource: ConfigObjectSource
  ): Resource[IO, HttpApp[IO]] =
    Resource
      .make(IO.delay(ActorSystem(BuildInfo.name))) { actorSystem =>
        IO.fromFuture(IO.delay(actorSystem.terminate())).productR(IO.unit)
      }
      .flatMap { implicit actorSystem =>
        Blocker
          .fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
          .product(Blocker.fromExecutorService(IO.delay(Executors.newWorkStealingPool())))
          .flatMap {
            case (ioBlocker, cpuBlocker) =>
              Resource
                .make {
                  ExternalComponents
                    .from[IO, IO](serviceConfiguration.applicationMode, configObjectSource, ioBlocker)
                } { _.shutdownHook }
                .map { externalComponents =>
                  val databaseUserDao = new DoobieUserDao(externalComponents.transactor)
                  val resetPasswordTokenDao = new DoobieResetPasswordTokenDao(externalComponents.transactor)
                  val weightEntryDao = new DoobieWeightEntryDao(externalComponents.transactor)
                  val lockedUserDao = new DoobieLockedUserDao(externalComponents.transactor)
                  val authenticationFailuresDao = new DoobieAuthenticationFailureDao(externalComponents.transactor)
                  val userAuthenticationConfigurationDao =
                    new DoobieUserAuthenticationConfigurationDao(externalComponents.transactor)

                  val passwordHashingService = new BCryptService[IO](cpuBlocker)
                  val authenticationTokenDao = new RedisAuthenticationTokenDao[IO](externalComponents.redisClient)
                  val authenticationSecretGenerator = new AuthenticationSecretGeneratorImpl[IO]

                  val staticResourceService = new StaticResourceService[IO](ioBlocker)

                  val healthCheckService = new HealthCheckServiceImpl[IO](
                    externalComponents.transactor,
                    externalComponents.redisClient,
                    externalComponents.publisher,
                    externalComponents.resourceService,
                    ioBlocker,
                    serviceConfiguration.applicationMode,
                    serviceConfiguration.buildInformation
                  )

                  val authenticationService = new AuthenticationServiceImpl(
                    passwordHashingService,
                    externalComponents.publisher,
                    databaseUserDao,
                    userAuthenticationConfigurationDao,
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
      }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ApiServiceConfiguration.load[IO](configObjectSource)

      _ <- application(serviceConfiguration, configObjectSource)
        .use { httpApp =>
          BlazeServerBuilder[IO]
            .withHttpApp(httpApp)
            .bindHttp(serviceConfiguration.httpConfiguration.port, "0.0.0.0")
            .withoutBanner
            .serve
            .compile
            .drain
        }
    } yield ExitCode.Success
}

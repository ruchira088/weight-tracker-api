package com.ruchij.web.routes

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import cats.~>
import com.ruchij.exceptions.ResourceNotFoundException
import com.ruchij.logging.Logger
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.user.models.User
import com.ruchij.web.middleware.authentication.AuthenticationTokenExtractor
import com.ruchij.web.middleware.correlation.CorrelationId.withId
import com.ruchij.web.requests.bodies.{LoginRequest, ResetPasswordRequest}
import com.ruchij.web.responses.ResetPasswordResponse
import com.ruchij.web.requests.RequestParser.Parser
import com.ruchij.web.routes.Paths.{`reset-password`, `user`}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}

import scala.language.higherKinds

object SessionRoutes {
  private val logger = Logger[SessionRoutes.type]

  def apply[F[_]: Sync: ValidatedNel[Throwable, *] ~> *[_]](authenticationService: AuthenticationService[F], authenticationTokenExtractor: AuthenticationTokenExtractor[F])(implicit dsl: Http4sDsl[F], authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = {
    import dsl._

    val publicRoutes: HttpRoutes[F] =
      HttpRoutes.of {
        case request @ POST -> Root withId correlationId =>
          for {
            LoginRequest(email, password, _) <- request.to[LoginRequest]

            _ <- logger.infoF[F](s"Attempting to login as email=$email")(correlationId)

            authenticationToken <- authenticationService.login(email, password)

            _ <- logger.infoF[F](s"Successfully logged in as email=$email")(correlationId)

            response <- Created(authenticationToken)
          } yield response

        case request @ POST -> Root / `reset-password` withId correlationId =>
          for {
            ResetPasswordRequest(email, frontEndUrl) <- request.to[ResetPasswordRequest]

            _ <- logger.infoF[F](s"Password reset requested for email=$email")(correlationId)

            resetPasswordToken <- authenticationService.resetPassword(email, frontEndUrl)

            _ <- logger.infoF[F](s"Password reset request successfully sent for email=$email")(correlationId)

            response <- Created(ResetPasswordResponse(email, resetPasswordToken.expiresAt, frontEndUrl))
          }
          yield response
      }

    val authenticatedRoutes: HttpRoutes[F] =
      authMiddleware {
        AuthedRoutes.of {
          case GET -> Root / `user` withId correlationId as authenticatedUser =>
            for {
              _ <- logger.infoF[F](s"Returning the authenticated user with email=${authenticatedUser.email}")(correlationId)
              response <- Ok(authenticatedUser)
            }
            yield response


          case authenticatedRequest @ DELETE -> Root withId correlationId as authenticatedUser =>
            for {
              _ <- logger.infoF[F](s"Logging out user with email=${authenticatedUser.email}")(correlationId)

              secret <-
                authenticationTokenExtractor.extract(authenticatedRequest.req)
                  .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("Unable to find authentication token")))

              authenticationToken <- authenticationService.logout(secret)

              _ <- logger.infoF[F](s"Successfully logged out user with email=${authenticatedUser.email}")(correlationId)

              response <- Ok(authenticationToken)
            }
            yield response
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}

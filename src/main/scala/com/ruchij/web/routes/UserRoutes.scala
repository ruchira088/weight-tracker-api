package com.ruchij.web.routes

import java.util.UUID

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import cats.~>
import com.ruchij.logging.Logger.LoggerOps
import com.ruchij.services.authorization.{AuthorizationService, Permission}
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.data.models.WeightEntry.weightEntryEncoder
import com.ruchij.services.user.UserService
import com.ruchij.services.user.models.User
import com.ruchij.web.middleware.authorization.Authorizer
import com.ruchij.web.middleware.correlation.CorrelationId.withId
import com.ruchij.web.requests.RequestParser._
import com.ruchij.web.requests.bodies.{CreateUserRequest, CreateWeightEntryRequest, UpdatePasswordRequest}
import com.ruchij.web.requests.queryparameters.QueryParameterMatcher.{PageNumberQueryParameterMatcher, PageSizeQueryParameterMatcher}
import com.ruchij.web.responses.PaginatedResultsResponse
import com.ruchij.web.routes.Paths.{`reset-password`, `weight-entry`}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import org.log4s.getLogger

import scala.language.higherKinds

object UserRoutes {
  private val logger = getLogger

  def apply[F[_]: Sync](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    authorizationService: AuthorizationService[F]
  )(
    implicit dsl: Http4sDsl[F],
    authMiddleware: AuthMiddleware[F, User],
    functionK: ValidatedNel[Throwable, *] ~> F
  ): HttpRoutes[F] = {
    import dsl._

    val authorizer: (User, UUID, Permission) => (=> F[Response[F]]) => F[Response[F]] =
      Authorizer.authorize(authorizationService)

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
      case request @ POST -> Root withId correlationId =>
        for {
          CreateUserRequest(email, password, firstName, lastName) <- request.to[CreateUserRequest]

          _ <- logger.infoF[F](s"Creating user with email=$email")(correlationId)

          user <- userService.create(email, password, firstName, lastName)

          _ <- logger.infoF[F](s"Created user with email=$email")(correlationId)

          response <- Created(user)
        } yield response

      case request @ PUT -> Root / UUIDVar(userId) / `reset-password` withId correlationId =>
        for {
          UpdatePasswordRequest(secret, password) <- request.to[UpdatePasswordRequest]

          _ <- logger.infoF[F](s"Resetting password for $userId...")(correlationId)

          updatedUser <- userService.updatePassword(userId, secret, password)

          _ <- logger.infoF[F](s"Resetted password for $userId")(correlationId)

          response <- Ok(updatedUser)
        } yield response
    }

    val authenticatedRoutes: HttpRoutes[F] =
      authMiddleware {
        AuthedRoutes.of {

          case GET -> Root / UUIDVar(userId) withId correlationId as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.READ) {
              for {
                _ <- logger.infoF[F](s"Fetching user with id=$userId for user with email=${authenticatedUser.email}")(correlationId)

                user <- userService.getById(userId)

                _ <- logger.infoF[F](s"Successfully fetched user with id=$userId for user with email=${authenticatedUser.email}")(correlationId)

                response <- Ok(user)
              } yield response
            }

          case authenticatedRequest @ POST -> Root / UUIDVar(userId) / `weight-entry` withId correlationId as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.WRITE) {
              for {
                CreateWeightEntryRequest(timestamp, weight, description) <- authenticatedRequest.req
                  .as[CreateWeightEntryRequest]

                _ <- logger.infoF[F](s"Inserting weight entry for email=${authenticatedUser.email}")(correlationId)

                weightEntry <- weightEntryService.create(timestamp, weight, description, userId, authenticatedUser.id)

                _ <- logger.infoF[F](s"Successfully inserted weight entry for email=${authenticatedUser.email}")(correlationId)

                response <- Created(weightEntry)
              } yield response
            }

          case GET -> Root / UUIDVar(userId) / `weight-entry` :? PageSizeQueryParameterMatcher(size) +& PageNumberQueryParameterMatcher(
                number
              ) withId correlationId as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.READ) {
              for {
                pageSize <- functionK(size)
                pageNumber <- functionK(number)

                _ <- logger.infoF[F](s"Fetching weight entries for email=${authenticatedUser.email} pageSize=$pageSize pageNumber=$pageNumber")(correlationId)

                weightEntryList <- weightEntryService.findByUser(userId, pageNumber, pageSize)

                _ <- logger.infoF[F](s"Fetched ${weightEntryList.size} weight entries for email=${authenticatedUser.email} pageSize=$pageSize pageNumber=$pageNumber")(correlationId)

                response <- Ok(PaginatedResultsResponse(weightEntryList, pageNumber, pageSize))
              } yield response
            }
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}

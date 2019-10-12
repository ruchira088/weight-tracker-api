package com.ruchij.web.routes

import java.util.UUID

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.authorization.{AuthorizationService, Permission}
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.data.models.WeightEntry.weightEntryEncoder
import com.ruchij.services.user.UserService
import com.ruchij.services.user.models.User
import com.ruchij.types.Transformation.~>
import com.ruchij.web.middleware.authorization.Authorizer
import com.ruchij.web.requests.RequestParser._
import com.ruchij.web.requests.bodies.{CreateUserRequest, CreateWeightEntryRequest}
import com.ruchij.web.requests.queryparameters.QueryParameterMatcher.{
  PageNumberQueryParameterMatcher,
  PageSizeQueryParameterMatcher
}
import com.ruchij.web.responses.PaginatedResultsResponse
import com.ruchij.web.routes.Paths.`weight-entry`
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes, Response}

import scala.language.higherKinds

object UserRoutes {

  def apply[F[_]: Sync](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    authorizationService: AuthorizationService[F]
  )(
    implicit dsl: Http4sDsl[F],
    authMiddleware: AuthMiddleware[F, User],
    transformation: ValidatedNel[Throwable, *] ~> F
  ): HttpRoutes[F] = {
    import dsl._

    val authorizer: (User, UUID, Permission) => (=> F[Response[F]]) => F[Response[F]] =
      Authorizer.authorize(authorizationService)

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          CreateUserRequest(email, password, firstName, lastName) <- request.to[CreateUserRequest]

          user <- userService.create(email, password, firstName, lastName)
          response <- Created(user)
        } yield response
    }

    val authenticatedRoutes: HttpRoutes[F] =
      authMiddleware {
        AuthedRoutes.of {
          case GET -> Root as authenticatedUser => Ok(authenticatedUser)

          case GET -> Root / UUIDVar(userId) as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.READ) {
              for {
                user <- userService.getById(userId)
                response <- Ok(user)
              } yield response
            }

          case authenticatedRequest @ POST -> Root / UUIDVar(userId) / `weight-entry` as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.WRITE) {
              for {
                CreateWeightEntryRequest(timestamp, weight, description) <- authenticatedRequest.req
                  .as[CreateWeightEntryRequest]
                weightEntry <- weightEntryService.create(timestamp, weight, description, userId, authenticatedUser.id)
                response <- Created(weightEntry)
              } yield response
            }

          case GET -> Root / UUIDVar(userId) / `weight-entry` :? PageSizeQueryParameterMatcher(size) +& PageNumberQueryParameterMatcher(
                number
              ) as authenticatedUser =>
            authorizer(authenticatedUser, userId, Permission.READ) {
              for {
                pageSize <- transformation(size)
                pageNumber <- transformation(number)
                weightEntryList <- weightEntryService.findByUser(userId, pageNumber, pageSize)
                response <- Ok(PaginatedResultsResponse(weightEntryList, pageNumber, pageSize))
              } yield response
            }
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}

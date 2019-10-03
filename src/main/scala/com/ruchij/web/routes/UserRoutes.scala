package com.ruchij.web.routes

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
import com.ruchij.web.requests.bodies.{CreateUserRequest, CreateWeightEntryRequest}
import com.ruchij.web.requests.queryparameters.QueryParameterMatcher.{PageNumberQueryParameterMatcher, PageSizeQueryParameterMatcher}
import com.ruchij.web.responses.{ExistsResponse, PaginatedResultsResponse}
import com.ruchij.web.routes.Paths.{`weight-entry`, exists}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import com.ruchij.web.requests.RequestParser._
import org.http4s.{AuthedRoutes, HttpRoutes}

import scala.language.higherKinds

object UserRoutes {

  def apply[F[_]: Sync](userService: UserService[F], weightEntryService: WeightEntryService[F])(
    implicit dsl: Http4sDsl[F],
    authMiddleware: AuthMiddleware[F, User],
    authorizationService: AuthorizationService[F],
    transformation: ValidatedNel[Throwable, *] ~> F
  ): HttpRoutes[F] = {
    import dsl._

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          CreateUserRequest(username, password, email, firstName, lastName) <- request.to[CreateUserRequest]

          user <- userService.create(username, password, email, firstName, lastName)
          response <- Created(user)
        } yield response
    }

    val authenticatedRoutes: HttpRoutes[F] =
      authMiddleware {
        AuthedRoutes.of {
          case GET -> Root as authenticatedUser => Ok(authenticatedUser)

          case GET -> Root / UUIDVar(userId) as authenticatedUser =>
            Authorizer.authorize(authenticatedUser, userId, Permission.READ) {
              for {
                user <- userService.getById(userId)
                response <- Ok(user)
              } yield response
            }

          case authenticatedRequest @ POST -> Root / UUIDVar(userId) / `weight-entry` as authenticatedUser =>
            Authorizer.authorize(authenticatedUser, userId, Permission.WRITE) {
              for {
                CreateWeightEntryRequest(timestamp, weight, description) <- authenticatedRequest.req
                  .as[CreateWeightEntryRequest]
                weightEntry <- weightEntryService.create(timestamp, weight, description, userId, authenticatedUser.id)
                response <- Created(weightEntry)
              } yield response
            }

          case GET -> Root / UUIDVar(userId) / `weight-entry` :? PageSizeQueryParameterMatcher(size) +& PageNumberQueryParameterMatcher(number) as authenticatedUser =>
            Authorizer.authorize(authenticatedUser, userId, Permission.READ) {
              for {
                pageSize <- transformation(size)
                pageNumber <- transformation(number)
                weightEntryList <- weightEntryService.findByUser(userId, pageNumber, pageSize)
                response <- Ok(PaginatedResultsResponse(weightEntryList, pageNumber, pageSize))
              }
              yield response
            }
        }
      }

    publicRoutes <+> authenticatedRoutes
  }

  def username[F[_]: Sync](userService: UserService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case GET -> Root / username / `exists` =>
        for {
          exists <- userService.findByUsername(username).isDefined
          response <- Ok(ExistsResponse(exists))
        }
        yield response
    }
  }
}

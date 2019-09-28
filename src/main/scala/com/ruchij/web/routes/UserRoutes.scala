package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.user.models.User
import com.ruchij.services.authorization.{AuthorizationService, Permission}
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.user.UserService
import com.ruchij.web.middleware.authorization.Authorizer
import com.ruchij.web.requests.{CreateUserRequest, CreateWeightEntryRequest}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}

import scala.language.higherKinds

object UserRoutes {
  def apply[F[_]: Sync](userService: UserService[F], weightEntryService: WeightEntryService[F])(
    implicit dsl: Http4sDsl[F],
    authMiddleware: AuthMiddleware[F, User],
    authorizationService: AuthorizationService[F]
  ): HttpRoutes[F] = {
    import dsl._

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          CreateUserRequest(username, password, email, firstName, lastName) <- request.as[CreateUserRequest]
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
              }
              yield response
            }

          case authenticatedRequest @ POST -> Root / UUIDVar(userId) / "weight-entry" as authenticatedUser =>
            Authorizer.authorize(authenticatedUser, userId, Permission.WRITE) {
              for {
                CreateWeightEntryRequest(timestamp, weight, description) <- authenticatedRequest.req.as[CreateWeightEntryRequest]
                weightEntry <- weightEntryService.create(timestamp, weight, description, userId, authenticatedUser.id)
                response <- Created(weightEntry)
              }
              yield response
            }
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}

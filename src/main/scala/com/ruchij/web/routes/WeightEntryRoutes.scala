package com.ruchij.web.routes

import com.ruchij.models.User
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

import scala.language.higherKinds

object WeightEntryRoutes {
  def apply[F[_]](implicit dsl: Http4sDsl[F], authMiddleware: AuthMiddleware[F, User]) = {
    import dsl._
  }
}

package com.ruchij.web.requests.bodies

import cats.effect.Sync
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class LoginRequest(email: String, password: String, keepMeLoggedIn: Option[Boolean])

object LoginRequest {
  implicit def loginRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]
}

package com.ruchij.web.requests

import cats.effect.Sync
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class CreateUserRequest(
  username: String,
  password: String,
  email: String,
  firstName: Option[String],
  lastName: Option[String]
)

object CreateUserRequest {
  implicit def createUserRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, CreateUserRequest] = jsonOf[F, CreateUserRequest]
}

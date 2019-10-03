package com.ruchij.web.requests.bodies

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import com.ruchij.web.requests.validators.Validator
import com.ruchij.web.requests.validators.Validator.StringValidationOps
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
  implicit def createUserRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, CreateUserRequest] =
    jsonOf[F, CreateUserRequest]

  implicit def createUserRequestValidator[F[_]: Sync: Lambda[X[_] => ValidatedNel[Throwable, *] ~> X]]
    : Validator[F, CreateUserRequest] =
    new Validator[F, CreateUserRequest] {
      override def validate[B <: CreateUserRequest](value: B): F[B] =
        Transformation[ValidatedNel[Throwable, *], F]
          .apply {
            value.username.isNotEmpty("username") |+|
              value.password.isNotEmpty("password") |+|
              value.email.isNotEmpty("email")
          }
          .as(value)
    }
}

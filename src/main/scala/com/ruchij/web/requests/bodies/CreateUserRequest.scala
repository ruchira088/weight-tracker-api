package com.ruchij.web.requests.bodies

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.circe.Decoders.{optionStringDecoder, taggedStringDecoder}
import com.ruchij.exceptions.ValidationException
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.web.requests.validators.Validator
import com.ruchij.web.requests.validators.Validator._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class CreateUserRequest(email: EmailAddress, password: String, firstName: String, lastName: Option[String])

object CreateUserRequest {
  implicit def createUserRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, CreateUserRequest] =
    jsonOf[F, CreateUserRequest]

  implicit val createUserRequestValidator: Validator[CreateUserRequest] =
    new Validator[CreateUserRequest] {
      override def validate[B <: CreateUserRequest](value: B): ValidatedNel[ValidationException, B] =
        (value.firstName as "firstName" mustBe nonEmpty) |+|
          (value.email as "email" mustBe validEmailAddress) |+|
          (value.password as "password" mustBe strongPassword) as value
    }
}

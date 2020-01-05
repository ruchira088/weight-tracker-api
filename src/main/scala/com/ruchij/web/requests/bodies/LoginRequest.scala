package com.ruchij.web.requests.bodies

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.circe.Decoders.taggedStringCirceDecoder
import com.ruchij.exceptions.ValidationException
import com.ruchij.types.Tags.EmailAddress
import com.ruchij.web.requests.validators.Validator
import com.ruchij.web.requests.validators.Validator._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class LoginRequest(email: EmailAddress, password: String, keepMeLoggedIn: Option[Boolean])

object LoginRequest {
  implicit def loginRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]

  implicit val loginRequestValidator: Validator[LoginRequest] = new Validator[LoginRequest] {
    override def validate[B <: LoginRequest](value: B): ValidatedNel[ValidationException, B] =
      (value.email.toString as "email" mustBe validEmailAddress) |+|
        (value.password as "password" mustBe nonEmpty) as value
  }
}

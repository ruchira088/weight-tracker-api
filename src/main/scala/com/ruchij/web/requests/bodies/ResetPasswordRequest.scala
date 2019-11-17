package com.ruchij.web.requests.bodies

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.circe.Decoders.taggedStringDecoder
import com.ruchij.exceptions.ValidationException
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.web.requests.validators.Validator
import com.ruchij.web.requests.validators.Validator._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class ResetPasswordRequest(email: EmailAddress, frontEndUrl: String)

object ResetPasswordRequest {
  implicit def resetPasswordRequestEntityDecoder[F[_]: Sync]: EntityDecoder[F, ResetPasswordRequest] =
    jsonOf[F, ResetPasswordRequest]

  implicit val resetPasswordRequestValidator: Validator[ResetPasswordRequest] =
    new Validator[ResetPasswordRequest] {
      override def validate[B <: ResetPasswordRequest](value: B): ValidatedNel[ValidationException, B] =
        (value.email as "email" mustBe validEmailAddress) as value
    }
}

package com.ruchij.web.requests.bodies

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import com.ruchij.exceptions.ValidationException
import com.ruchij.web.requests.validators.Validator
import com.ruchij.web.requests.validators.Validator._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class UpdatePasswordRequest(secret: String, password: String)

object UpdatePasswordRequest {
  implicit def updatePasswordRequest[F[_]: Sync]: EntityDecoder[F, UpdatePasswordRequest] =
    jsonOf[F, UpdatePasswordRequest]

  implicit val updatePasswordRequestValidator: Validator[UpdatePasswordRequest] =
    new Validator[UpdatePasswordRequest] {
      override def validate[B <: UpdatePasswordRequest](value: B): ValidatedNel[ValidationException, B] =
        (value.secret as "secret" mustBe nonEmpty) |+|
          (value.password as "password" mustBe strongPassword) as value
    }
}

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

case class UnlockUserRequest(unlockCode: String)

object UnlockUserRequest {
  implicit def unlockUserRequestDecoder[F[_]: Sync]: EntityDecoder[F, UnlockUserRequest] =
    jsonOf[F, UnlockUserRequest]

  implicit val unlockUserRequestValidator: Validator[UnlockUserRequest] = new Validator[UnlockUserRequest] {
    override def validate[B <: UnlockUserRequest](value: B): ValidatedNel[ValidationException, B] =
      value.unlockCode as "unlockCode" mustBe nonEmpty as value
  }
}

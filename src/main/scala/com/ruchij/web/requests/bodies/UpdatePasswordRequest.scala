package com.ruchij.web.requests.bodies

import cats.effect.Sync
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.language.higherKinds

case class UpdatePasswordRequest(secret: String, password: String)

object UpdatePasswordRequest {
  implicit def updatePasswordRequest[F[_]: Sync]: EntityDecoder[F, UpdatePasswordRequest] =
    jsonOf[F, UpdatePasswordRequest]
}

package com.ruchij.circe

import cats.Applicative
import com.ruchij.models.User
import org.http4s.circe.jsonEncoderOf
import io.circe.generic.auto._
import org.http4s.EntityEncoder

import scala.language.higherKinds

object EntityEncoders {
  implicit def userEncoder[F[_]: Applicative]: EntityEncoder[F, User] = jsonEncoderOf[F, User]
}

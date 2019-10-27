package com.ruchij.services.email

import com.ruchij.services.email.models.Email

import scala.language.higherKinds

trait EmailService[F[_]] {
  type Response

  def send(email: Email): F[Response]
}

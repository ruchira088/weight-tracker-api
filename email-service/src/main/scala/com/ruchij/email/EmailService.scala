package com.ruchij.email

import scala.language.higherKinds

trait EmailService[F[_]] {
  type Response

  def send(email: models.Email): F[Response]
}

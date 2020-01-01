package com.ruchij.email

import cats.effect.Sync

import scala.language.higherKinds

class ConsoleEmailService[F[_]: Sync] extends EmailService[F] {
  override type Response = Unit

  override def send(email: models.Email): F[Unit] =
    Sync[F].delay {
      println {
        s"""
          |-------------------------------------------------------
          |TO: ${email.to}
          |FROM: ${email.from}
          |SUBJECT: ${email.subject}
          |BODY:
          |${email.content.body}
          |-------------------------------------------------------
          |""".stripMargin
      }
    }
}

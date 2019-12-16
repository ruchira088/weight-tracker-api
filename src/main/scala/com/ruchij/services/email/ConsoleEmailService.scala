package com.ruchij.services.email
import cats.effect.Sync
import com.ruchij.services.email.models.Email

import scala.language.higherKinds

class ConsoleEmailService[F[_]: Sync] extends EmailService[F] {
  override type Response = Unit

  override def send(email: Email): F[Unit] =
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

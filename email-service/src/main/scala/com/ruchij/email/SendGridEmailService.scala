package com.ruchij.email

import cats.effect.{Blocker, ContextShift, Sync}
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Content, Email => SendGridEmail}
import com.sendgrid.{Method, Request, SendGrid, Response => SendGridResponse}

import scala.language.higherKinds

class SendGridEmailService[F[_]: ContextShift: Sync](sendGrid: SendGrid, blocker: Blocker)
    extends EmailService[F] {
  override type Response = SendGridResponse

  override def send(email: models.Email): F[Response] =
    blocker
      .delay {
        sendGrid.api {
          new Request {
            setMethod(Method.POST)
            setEndpoint("mail/send")
            setBody {
              new Mail(
                new SendGridEmail(email.from),
                email.subject,
//                new SendGridEmail(email.to),
                new SendGridEmail("ruchira088@gmail.com"),
                new Content(email.content.contentType, email.content.body)
              ).build()
            }
          }
        }
      }
}

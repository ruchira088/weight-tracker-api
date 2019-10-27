package com.ruchij.services.email

import cats.effect.{Blocker, ContextShift, Sync}
import com.ruchij.services.email.models.Email
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Content, Email => SendGridEmail}
import com.sendgrid.{Method, Request, SendGrid, Response => SendGridResponse}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

class SendGridEmailService[F[_]: ContextShift: Sync](sendGrid: SendGrid, ioBlockingExecutionContext: ExecutionContext)
    extends EmailService[F] {
  override type Response = SendGridResponse

  override def send(email: Email): F[Response] =
    Blocker
      .liftExecutionContext(ioBlockingExecutionContext)
      .delay {
        sendGrid.api {
          new Request {
            setMethod(Method.POST)
            setEndpoint("mail/send")
            setBody {
              new Mail(
                new SendGridEmail(email.to),
                email.subject,
                new SendGridEmail(email.from),
                new Content(email.content.contentType, email.content.body)
              ).build()
            }
          }
        }
      }
}

package com.ruchij.services.email.models

import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.user.models.User
import html.Welcome
import play.twirl.api.HtmlFormat
import shapeless.tag.@@
import shapeless.tag

case class Email(to: EmailAddress, from: EmailAddress, subject: String, content: HtmlFormat.Appendable)

object Email {
  trait EmailAddressTag
  type EmailAddress = String @@ EmailAddressTag

  def emailAddress(email: String): EmailAddress = tag[EmailAddressTag][String](email)

  def welcomeEmail(user: User): Email =
    Email(
      user.email,
      emailAddress("Weight Tracker <welcome@weight-tracker.ruchij.com>"),
      "Welcome to Weight Tracker",
      Welcome(user)
    )
}

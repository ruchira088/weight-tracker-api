package com.ruchij.services.email.models

import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.user.models.User
import play.twirl.api.HtmlFormat
import shapeless.tag.@@

case class Email(to: EmailAddress, from: EmailAddress, subject: String, content: HtmlFormat.Appendable)

object Email {
  trait EmailAddressTag
  type EmailAddress = String @@ EmailAddressTag

  def welcomeEmail(user: User): Email = ???
}

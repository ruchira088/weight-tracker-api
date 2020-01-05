package com.ruchij.email.models

import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.services.authentication.models.ResetPasswordToken
import com.ruchij.services.user.models.User
import com.ruchij.types.Tags.{EmailAddress, emailAddress}
import html._
import play.twirl.api.HtmlFormat

case class Email(to: EmailAddress, from: EmailAddress, subject: String, content: HtmlFormat.Appendable)

object Email {
  def welcomeEmail(user: User): Email =
    Email(
      user.email,
      emailAddress("Weight Tracker <welcome@weight-tracker.ruchij.com>"),
      "Welcome to Weight Tracker",
      Welcome(user)
    )

  def resetPassword(user: User, resetPasswordToken: ResetPasswordToken, frontEndUrl: String): Email =
    Email(
      user.email,
      emailAddress("Weight Tracker <reset.password@weight-tracker.ruchij.com>"),
      "Reset your password",
      ResetPassword(user, resetPasswordToken, frontEndUrl)
    )

  def unlockUser(user: User, lockedUser: DatabaseLockedUser): Email =
    Email(
      user.email,
      emailAddress("Weight Tracker <locked.user@weight-tracker.ruchij.com>"),
      "Locked user account",
      LockedUser(user, lockedUser)
    )
}

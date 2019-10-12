package com.ruchij.web.requests.validators

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._
import com.ruchij.exceptions.ValidationException
import org.apache.commons.validator.routines.EmailValidator

trait Validator[-A] {
  def validate[B <: A](value: B): ValidatedNel[ValidationException, B]
}

object Validator {
  def apply[A](implicit validator: Validator[A]): Validator[A] = validator

  def predicate[A](value: A, condition: Boolean, errorMessage: => String): ValidatedNel[ValidationException, A] =
    if (condition) Valid(value) else Invalid(NonEmptyList.of(ValidationException(errorMessage)))

  implicit class ValidationInfo[+A](val value: A) extends AnyVal {
    def as(key: String): ValidationOperation[A] = ValidationOperation(key, value)
  }

  case class ValidationOperation[+A](key: String, value: A) {
    def mustBe(valueValidator: ValueValidator[A]): ValidatedNel[ValidationException, A] =
      valueValidator.validate(key, value)
  }

  trait ValueValidator[-A] {
    def validate[B <: A](key: String, value: B): ValidatedNel[ValidationException, B]
  }

  val nonEmpty: ValueValidator[String] =
    new ValueValidator[String] {
      override def validate[B <: String](key: String, value: B): ValidatedNel[ValidationException, B] =
        predicate(value, value.trim.nonEmpty, s"$key must not be empty")
    }

  val validEmailAddress: ValueValidator[String] =
    new ValueValidator[String] {
      override def validate[B <: String](key: String, value: B): ValidatedNel[ValidationException, B] =
        predicate(value, EmailValidator.getInstance().isValid(value), s"$key does not have a valid email address")
    }

  val strongPassword: ValueValidator[String] =
    new ValueValidator[String] {
      override def validate[B <: String](key: String, value: B): ValidatedNel[ValidationException, B] = {
        val length =
          predicate(List(value), value.trim.length >= 8, s"$key must contain at least 8 characters")

        val containsLetter =
          predicate(List(value), value.exists(_.isLetter), s"$key must contain at least one letter")

        val containsDigit =
          predicate(List(value), value.exists(_.isDigit), s"$key must contain at least one digit")

        val containsSpecialChar =
          predicate(List(value), !value.forall(_.isLetterOrDigit), s"$key must contain at least one special character")

        (length |+| containsLetter |+| containsDigit |+| containsSpecialChar) as value
      }
    }
}


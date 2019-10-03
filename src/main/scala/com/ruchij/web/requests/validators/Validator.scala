package com.ruchij.web.requests.validators

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import com.ruchij.exceptions.ValidationException

import scala.language.higherKinds

trait Validator[F[_], -A] {
  def validate[B <: A](value: B): F[B]
}

object Validator {
  def apply[F[_], A](implicit validator: Validator[F, A]): Validator[F, A] = validator

  implicit class StringValidationOps(val string: String) extends AnyVal {
    def isNotEmpty(fieldName: String): ValidatedNel[Throwable, Unit] =
      string.trim.nonEmpty ? s"$fieldName must not be empty"
  }

  implicit class ValidationOps(val condition: Boolean) extends AnyVal {
    def ?(validationError: String): ValidatedNel[Throwable, Unit] =
      if (condition) Valid((): Unit) else Invalid(NonEmptyList.of(ValidationException(validationError)))
  }
}

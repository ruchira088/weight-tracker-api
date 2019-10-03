package com.ruchij.exceptions

case class ValidationException(message: String) extends Exception(message)

object ValidationException {
  def unapply(throwable: Throwable): Option[Throwable] =
    throwable match {
      case _: ValidationException => Some(throwable)

      case AggregatedException(throwables) =>
          if (throwables.forall {
            case _: ValidationException => true
            case _ => false
          }) Some(throwable) else None

      case _ => None
    }
}

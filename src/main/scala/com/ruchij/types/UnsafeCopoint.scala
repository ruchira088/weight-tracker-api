package com.ruchij.types

import cats.effect.IO

import scala.language.higherKinds
import scala.util.Try

trait UnsafeCopoint[M[_]] {
  def extract[A](value: M[A]): A
}

object UnsafeCopoint {
  def unsafeExtract[M[_], A](value: M[A])(implicit unsafeCopoint: UnsafeCopoint[M]): A = unsafeCopoint.extract(value)

  implicit val tryUnsafeCopoint: UnsafeCopoint[Try] = new UnsafeCopoint[Try] {
    override def extract[A](value: Try[A]): A = value.get
  }

  implicit val ioUnsafeCopoint: UnsafeCopoint[IO] = new UnsafeCopoint[IO] {
    override def extract[A](value: IO[A]): A = value.unsafeRunSync()
  }
}

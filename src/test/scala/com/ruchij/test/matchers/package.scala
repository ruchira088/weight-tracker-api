package com.ruchij.test

import cats.effect.IO

import scala.language.higherKinds

package object matchers {

  def matchWith[A](expected: A): IoResultMatcher[A] = new IoResultMatcher(expected)

  def beJsonResponse[F[_]]: JsonContentTypeHeaderMatcher[F] = new JsonContentTypeHeaderMatcher[F]
}

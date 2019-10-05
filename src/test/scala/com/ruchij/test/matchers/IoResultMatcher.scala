package com.ruchij.test.matchers

import cats.effect.IO
import org.scalatest.matchers.{MatchResult, Matcher}

class IoResultMatcher[A](right: IO[A]) extends Matcher[IO[A]] {
  override def apply(left: IO[A]): MatchResult = {
    val expected = right.unsafeRunSync()
    val actual = left.unsafeRunSync()

    MatchResult(expected == actual, s"$actual does NOT equal $expected", s"$actual does equal $expected")
  }
}

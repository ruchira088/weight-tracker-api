package com.ruchij

import org.scalatest.{FlatSpec, MustMatchers}

class ExampleTest extends FlatSpec with MustMatchers {
  "Sample test" should "pass nicely" in {
    8 mustBe 8
  }
}

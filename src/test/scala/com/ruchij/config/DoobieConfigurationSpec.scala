package com.ruchij.config

import cats.effect.IO
import com.ruchij.types.FunctionKTypes.configReaderResultToIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import pureconfig.ConfigSource

class DoobieConfigurationSpec extends AnyFlatSpec with Matchers {
  "DoobieConfiguration.load" should "parse the configuration" in {
    val configuration =
      """
        | doobie-configuration {
        |   driver = "org.postgresql.Driver"
        |   url = "jdbc:postgresql://localhost:5432/weight-tracker"
        |   user = "john.doe"
        |   password = "Pa$$w0rd"
        | }
        |""".stripMargin

    val expectedDoobieConfiguration =
      DoobieConfiguration(
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/weight-tracker",
        "john.doe",
        "Pa$$w0rd"
      )

    DoobieConfiguration.load[IO](ConfigSource.string(configuration)).unsafeRunSync() mustBe expectedDoobieConfiguration
  }
}

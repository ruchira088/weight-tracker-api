package com.ruchij

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.ruchij.test.utils.RandomGenerator

import scala.language.higherKinds

object ScratchPad extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    IO(println(RandomGenerator.databaseUser())).as(ExitCode.Success)
}

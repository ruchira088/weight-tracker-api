package com.ruchij.test.utils

import cats.{Applicative, Id, Monad}
import cats.implicits._
import cats.effect.Sync
import com.github.javafaker.Faker
import com.ruchij.types.Random

import scala.language.higherKinds
import scala.util.{Random => ScalaRandom}

object RandomGenerator {
  type RandomValue[+A] = Random[Id, A]

  val faker: Faker = Faker.instance()

  import faker._

  def random[A](randomValue: => A): RandomValue[A] = new Random[Id, A] {
    override def value[B >: A]: Id[B] = randomValue
  }

  def username(): RandomValue[String] = random(name().username())

  def password(): RandomValue[String] = random(internet().password())

  def email(): RandomValue[String] = random(internet().emailAddress())

  def firstName(): RandomValue[String] = random(name().firstName())

  def lastName(): RandomValue[String] = random(name().lastName())

  def boolean[F[_]: Applicative]: Random[F, Boolean] =
    new Random[F, Boolean] {
      override def value[B >: Boolean]: F[B] = Applicative[F].pure(ScalaRandom.nextBoolean())
    }

  def option[F[_]: Monad, A](random: Random[F, A]): Random[F, Option[A]] =
    new Random[F, Option[A]] {
      override def value[B >: Option[A]]: F[B] =
        boolean[F].value.flatMap { if (_) random.value.map(Some.apply) else Applicative[F].pure(None) }
    }
}

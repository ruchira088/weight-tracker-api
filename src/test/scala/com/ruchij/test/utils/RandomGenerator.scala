package com.ruchij.test.utils

import java.util.UUID

import cats.Applicative
import com.github.javafaker.Faker
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.services.email.models.Email.{EmailAddress, EmailAddressTag}
import com.ruchij.services.user.models.User
import com.ruchij.types.Random
import org.joda.time.DateTime
import shapeless.tag

import scala.language.higherKinds
import scala.util.{Random => ScalaRandom}

object RandomGenerator {
  val faker: Faker = Faker.instance()

  import faker._

  val PASSWORD = "Pa$$w0rd"
  val SALTED_PASSWORD = "$2a$10$afXIMtS3CcuWZMdDOkBmdOdGlnivbG/IgFtilPDvK40BHt8a7QUV2"

  def email(): EmailAddress = tag[EmailAddressTag][String](internet().emailAddress())

  def password(): String = "5ca|a" + internet().password(8, 100, true, true, true)

  def firstName(): String = name().firstName()

  def lastName(): String = name().lastName()

  def uuid(): UUID = UUID.randomUUID()

  def user(): User =
    User(uuid(), email(), firstName(), option(lastName()))

  def databaseUser(): DatabaseUser =
    DatabaseUser(uuid(), DateTime.now(), email(), SALTED_PASSWORD, firstName(), option(lastName()))

  def databaseAuthenticationToken(userId: UUID): DatabaseAuthenticationToken =
    DatabaseAuthenticationToken(userId, DateTime.now(), DateTime.now().plusSeconds(30), 0, uuid().toString, None)

  def databaseWeightEntry(userId: UUID): DatabaseWeightEntry =
    DatabaseWeightEntry(uuid(), 0, DateTime.now(), userId, userId, DateTime.now(), weight(), option(description()))

  def weight(): Double = "%.2f".format(ScalaRandom.nextInt(5000).toDouble / 100 + 50).toDouble

  def boolean(): Boolean = ScalaRandom.nextBoolean()

  def description(): String =
    choose(
      faker.rickAndMorty().quote(),
      faker.gameOfThrones().quote(),
      faker.chuckNorris().fact(),
      faker.howIMetYourMother().quote(),
      faker.superhero().descriptor(),
      faker.shakespeare().romeoAndJulietQuote()
    ).replaceAll("’", "'").replaceAll("…", ".")

  def choose[A](values: A*): A = values(ScalaRandom.nextInt(values.length))

  def option[A](value: => A): Option[A] = if (boolean()) Some(value) else None

  def random[F[_]: Applicative, A](result: A): Random[F, A] =
    new Random[F, A] {
      override def value[B >: A]: F[B] = Applicative[F].pure[B](result)
    }
}

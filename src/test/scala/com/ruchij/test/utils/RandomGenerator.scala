package com.ruchij.test.utils

import java.util.UUID

import com.github.javafaker.Faker
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.user.models.User
import org.joda.time.DateTime

import scala.util.{Random => ScalaRandom}

object RandomGenerator {
  val faker: Faker = Faker.instance()

  import faker._

  val PASSWORD = "Pa$$w0rd"
  val SALTED_PASSWORD = "$2a$10$afXIMtS3CcuWZMdDOkBmdOdGlnivbG/IgFtilPDvK40BHt8a7QUV2"

  def email(): String = internet().emailAddress()

  def password(): String = internet().password()

  def firstName(): String = name().firstName()

  def lastName(): String = name().lastName()

  def uuid(): UUID = UUID.randomUUID()

  def user(): User =
    User(uuid(), email(), firstName(), option(lastName()))

  def databaseUser(): DatabaseUser =
    DatabaseUser(uuid(), DateTime.now(), email(), SALTED_PASSWORD, firstName(), option(lastName()))

  def authenticationToken(userId: UUID): AuthenticationToken =
    AuthenticationToken(userId, DateTime.now().plusSeconds(30), uuid().toString)

  def boolean(): Boolean = ScalaRandom.nextBoolean()

  def option[A](value: => A): Option[A] = if (boolean()) Some(value) else None
}

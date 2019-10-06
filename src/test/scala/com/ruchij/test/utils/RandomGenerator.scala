package com.ruchij.test.utils

import java.util.UUID

import com.github.javafaker.Faker
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.services.user.models.User
import org.joda.time.DateTime

import scala.language.higherKinds
import scala.util.{Random => ScalaRandom}

object RandomGenerator {
  val faker: Faker = Faker.instance()

  import faker._

  def username(): String = name().username()

  def password(): String = internet().password()

  def email(): String = internet().emailAddress()

  def firstName(): String = name().firstName()

  def lastName(): String = name().lastName()

  def uuid(): UUID = UUID.randomUUID()

  def user(): User =
    User(uuid(), username(), email(), option(firstName()), option(lastName()))

  def databaseUser(): DatabaseUser =
    DatabaseUser(uuid(), DateTime.now(), username(), password(), email(), option(firstName()), option(lastName()))

  def boolean(): Boolean = ScalaRandom.nextBoolean()

  def option[A](value: => A): Option[A] = if (boolean()) Some(value) else None
}
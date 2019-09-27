package com.ruchij.models

import java.util.UUID

import cats.Applicative
import com.ruchij.daos.user.models.DatabaseUser
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class User(id: UUID, username: String, email: String, firstName: Option[String], lastName: Option[String])

object User {
  def fromDatabaseUser(databaseUser: DatabaseUser): User =
    User(databaseUser.id, databaseUser.username, databaseUser.email, databaseUser.firstName, databaseUser.lastName)

  implicit def userEntityEncoder[F[_]: Applicative]: EntityEncoder[F, User] = jsonEncoderOf[F, User]
}

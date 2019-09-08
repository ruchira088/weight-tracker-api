package com.ruchij.services.user

import cats.implicits._
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import com.ruchij.daos.user.DatabaseUserDao
import com.ruchij.exceptions.ResourceConflictException
import com.ruchij.models.User

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class UserServiceImpl[F[_]: Sync](databaseUserDao: DatabaseUserDao[F]) extends UserService[F] {
  override def create(
    username: String,
    password: String,
    email: String,
    firstName: Option[String],
    lastName: Option[String]
  )(implicit clock: Clock[F]): F[User] =
    for {
      usernameExists <- findByUsername(username).isDefined
      _ <- if (usernameExists) Sync[F].raiseError[Unit](ResourceConflictException(s"Username = $username")) else Sync[F].unit

      emailExists <- findByEmail(email).isDefined
      _ <- if (emailExists) Sync[F].raiseError[Unit](ResourceConflictException(s"Email = $email")) else Sync[F].unit

      timestamp <- clock.realTime(MILLISECONDS)
    }
    yield ???

  override def findByUsername(username: String): OptionT[F, User] =
    databaseUserDao.findByUsername(username)
      .map(User.fromDatabaseUser)

  override def findByEmail(email: String): OptionT[F, User] =
    databaseUserDao.findByEmail(email)
      .map(User.fromDatabaseUser)
}

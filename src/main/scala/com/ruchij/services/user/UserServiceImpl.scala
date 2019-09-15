package com.ruchij.services.user

import cats.implicits._
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import com.ruchij.daos.user.DatabaseUserDao
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.ResourceConflictException
import com.ruchij.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.types.RandomUuid
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class UserServiceImpl[F[_]: Sync: Clock: RandomUuid](
  databaseUserDao: DatabaseUserDao[F],
  authenticationService: AuthenticationService[F]
) extends UserService[F] {

  override def create(
    username: String,
    password: String,
    email: String,
    firstName: Option[String],
    lastName: Option[String]
  ): F[User] =
    for {
      usernameExists <- findByUsername(username).isDefined
      _ <- if (usernameExists) Sync[F].raiseError[Unit](ResourceConflictException(s"username already exists: $username")) else Sync[F].unit

      emailExists <- findByEmail(email).isDefined
      _ <- if (emailExists) Sync[F].raiseError[Unit](ResourceConflictException(s"email already exists: $email")) else Sync[F].unit

      hashedPassword <- authenticationService.hashPassword(password)
      timestamp <- Clock[F].realTime(MILLISECONDS)
      id <- RandomUuid[F].uuid

      databaseUser <- databaseUserDao.insert(
        DatabaseUser(id, new DateTime(timestamp), username, hashedPassword, email, firstName, lastName)
      )
    } yield User.fromDatabaseUser(databaseUser)

  override def findByUsername(username: String): OptionT[F, User] =
    databaseUserDao
      .findByUsername(username)
      .map(User.fromDatabaseUser)

  override def findByEmail(email: String): OptionT[F, User] =
    databaseUserDao
      .findByEmail(email)
      .map(User.fromDatabaseUser)
}

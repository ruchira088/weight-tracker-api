package com.ruchij.services.user

import java.util.UUID

import cats.implicits._
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import com.ruchij.daos.user.UserDao
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.{InternalServiceException, ResourceConflictException, ResourceNotFoundException}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.types.Random
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class UserServiceImpl[F[_]: Sync: Clock: Lambda[X[_] => Random[X, UUID]]](
  databaseUserDao: UserDao[F],
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
      _ <- if (usernameExists)
        Sync[F].raiseError[Unit](ResourceConflictException(s"username already exists: $username"))
      else Sync[F].unit

      emailExists <- findByEmail(email).isDefined
      _ <- if (emailExists) Sync[F].raiseError[Unit](ResourceConflictException(s"email already exists: $email"))
      else Sync[F].unit

      hashedPassword <- authenticationService.hashPassword(password)
      timestamp <- Clock[F].realTime(MILLISECONDS)
      id <- Random[F, UUID].value

      _ <- databaseUserDao.insert(
        DatabaseUser(id, new DateTime(timestamp), username, hashedPassword, email, firstName, lastName)
      )

      user <-
        getById(id).adaptError {
          case _: ResourceNotFoundException => InternalServiceException("Unable to persist user")
        }
    } yield user

  override def getById(id: UUID): F[User] =
    databaseUserDao.findById(id)
      .map(User.fromDatabaseUser)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"User not found for id = $id")))

  override def findByUsername(username: String): OptionT[F, User] =
    databaseUserDao.findByUsername(username).map(User.fromDatabaseUser)

  override def findByEmail(email: String): OptionT[F, User] =
    databaseUserDao.findByEmail(email).map(User.fromDatabaseUser)
}

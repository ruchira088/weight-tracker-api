package com.ruchij.services.user

import java.util.UUID

import cats.implicits._
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import com.ruchij.daos.user.UserDao
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.{AuthenticationException, InternalServiceException, ResourceConflictException, ResourceNotFoundException}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.types.Random
import com.ruchij.types.Utils.predicate
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class UserServiceImpl[F[_]: Sync: Clock: Random[*[_], UUID]](
  databaseUserDao: UserDao[F],
  authenticationService: AuthenticationService[F],
  emailService: EmailService[F]
) extends UserService[F] {

  override def create(email: EmailAddress, password: String, firstName: String, lastName: Option[String]): F[User] =
    for {
      emailExists <- findByEmail(email).isDefined
      _ <- predicate(emailExists, ResourceConflictException(s"email already exists: $email"))

      hashedPassword <- authenticationService.hashPassword(password)
      timestamp <- Clock[F].realTime(MILLISECONDS)
      id <- Random[F, UUID].value

      _ <- databaseUserDao.insert {
        DatabaseUser(id, new DateTime(timestamp), email, hashedPassword, firstName, lastName)
      }

      user <- getById(id).adaptError {
        case _: ResourceNotFoundException => InternalServiceException("Unable to persist user")
      }

      _ <- emailService.send(Email.welcomeEmail(user))

    } yield user

  override def getById(id: UUID): F[User] =
    databaseUserDao
      .findById(id)
      .map(User.fromDatabaseUser)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"User not found for id = $id")))

  override def findByEmail(email: EmailAddress): OptionT[F, User] =
    databaseUserDao.findByEmail(email).map(User.fromDatabaseUser)

  override def updatePassword(userId: UUID, secret: String, password: String): F[User] =
    for {
      resetPasswordToken <- authenticationService.getResetPasswordToken(userId, secret)
      _ <- predicate(resetPasswordToken.used, AuthenticationException("Token has already been used"))

      timestamp <- Clock[F].realTime(MILLISECONDS)
      _ <- predicate(resetPasswordToken.expiresAt.isBefore(timestamp), AuthenticationException("Token is expired"))

      hashedPassword <- authenticationService.hashPassword(password)
      success <- databaseUserDao.updatePassword(userId, hashedPassword)
      _ <- predicate(!success, ResourceNotFoundException(s"User not found for id = $userId"))
      _ <- authenticationService.passwordResetCompleted(userId, secret)

      updatedUser <- getById(userId)
    }
    yield updatedUser
}

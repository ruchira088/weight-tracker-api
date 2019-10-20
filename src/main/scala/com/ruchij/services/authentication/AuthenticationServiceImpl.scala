package com.ruchij.services.authentication

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.resetpassword.ResetPasswordTokenDao
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.daos.user.UserDao
import com.ruchij.exceptions.{AuthenticationException, ResourceNotFoundException}
import com.ruchij.services.authentication.models.{AuthenticationToken, ResetPasswordToken}
import com.ruchij.services.hashing.PasswordHashingService
import com.ruchij.services.user.models.User
import com.ruchij.types.Utils.predicate
import org.joda.time.DateTime

import scala.language.higherKinds

class AuthenticationServiceImpl[F[_]: Sync: Clock](
  passwordHashingService: PasswordHashingService[F, String],
  userDao: UserDao[F],
  resetPasswordTokenDao: ResetPasswordTokenDao[F],
  authenticationTokenDao: AuthenticationTokenDao[F],
  authenticationSecretGenerator: AuthenticationSecretGenerator[F],
  authenticationConfiguration: AuthenticationConfiguration
) extends AuthenticationService[F] {

  override def hashPassword(password: String): F[String] = passwordHashingService.hash(password)

  override def login(email: String, password: String): F[AuthenticationToken] =
    for {
      databaseUser <- userDao
        .findByEmail(email)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"Email not found: $email")))

      isSuccess <- passwordHashingService.checkPassword(password, databaseUser.password)
      _ <- predicate(!isSuccess, AuthenticationException("Invalid credentials"))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      secret <- authenticationSecretGenerator.generate(User.fromDatabaseUser(databaseUser))

      databaseAuthenticationToken <- authenticationTokenDao.createToken {
        DatabaseAuthenticationToken(
          databaseUser.id,
          new DateTime(timestamp),
          new DateTime(timestamp + authenticationConfiguration.sessionTimeout.toMillis),
          0,
          secret,
          None
        )
      }
    } yield AuthenticationToken.fromDatabaseAuthenticationToken(databaseAuthenticationToken)

  override def logout(secret: String): F[AuthenticationToken] =
    authenticationTokenDao.remove(secret).map(AuthenticationToken.fromDatabaseAuthenticationToken)

  override def authenticate(secret: String): F[User] =
    for {
      authenticationToken <- authenticationTokenDao
        .find(secret)
        .getOrElseF(Sync[F].raiseError(AuthenticationException("Invalid credentials")))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      _ <- predicate(authenticationToken.expiresAt.isBefore(timestamp), AuthenticationException("Expired credentials"))

      databaseUser <- userDao
        .findById(authenticationToken.userId)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("User not found")))

      _ <- authenticationTokenDao.extendExpiry(authenticationToken.secret, authenticationConfiguration.sessionTimeout)
    } yield User.fromDatabaseUser(databaseUser)

  override def resetPassword(email: String): F[ResetPasswordToken] =
    for {
      databaseUser <- userDao
        .findByEmail(email)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"$email was not found")))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      secret <- authenticationSecretGenerator.generate(User.fromDatabaseUser(databaseUser))

      databaseResetPasswordToken =
        DatabaseResetPasswordToken(
          databaseUser.id,
          secret,
          new DateTime(timestamp),
          new DateTime(timestamp + authenticationConfiguration.sessionTimeout.toMillis),
          None
        )

      _ <- resetPasswordTokenDao.insert(databaseResetPasswordToken)

    } yield ResetPasswordToken.fromDatabaseResetPasswordToken(databaseResetPasswordToken)

  override def getResetPasswordToken(userId: UUID, secret: String): F[ResetPasswordToken] =
    resetPasswordTokenDao.find(userId, secret)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("Reset password token not found")))
      .map(ResetPasswordToken.fromDatabaseResetPasswordToken)

  override def passwordResetCompleted(userId: UUID, secret: String): F[ResetPasswordToken] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      success <- resetPasswordTokenDao.resetCompleted(userId, secret, new DateTime(timestamp))

      _ <- predicate(!success, ResourceNotFoundException("Reset password token not found"))
      resetPasswordToken <- getResetPasswordToken(userId, secret)
    }
    yield resetPasswordToken

}

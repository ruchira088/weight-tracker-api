package com.ruchij.services.authentication

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.authenticationfailure.AuthenticationFailureDao
import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.lockeduser.LockedUserDao
import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.daos.resetpassword.ResetPasswordTokenDao
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.daos.user.UserDao
import com.ruchij.exceptions.{AuthenticationException, LockedUserAccountException, ResourceNotFoundException}
import com.ruchij.services.authentication.models.{AuthenticationToken, ResetPasswordToken}
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.hashing.PasswordHashingService
import com.ruchij.services.user.models.User
import com.ruchij.types.Random
import com.ruchij.types.Tags.EmailAddress
import com.ruchij.types.Utils.predicate
import org.joda.time.DateTime

import scala.language.higherKinds

class AuthenticationServiceImpl[F[_]: Sync: Clock: Random[*[_], UUID]](
  passwordHashingService: PasswordHashingService[F, String],
  emailService: EmailService[F],
  userDao: UserDao[F],
  lockedUserDao: LockedUserDao[F],
  authenticationFailureDao: AuthenticationFailureDao[F],
  resetPasswordTokenDao: ResetPasswordTokenDao[F],
  authenticationTokenDao: AuthenticationTokenDao[F],
  authenticationSecretGenerator: AuthenticationSecretGenerator[F],
  authenticationConfiguration: AuthenticationConfiguration
) extends AuthenticationService[F] {

  override def hashPassword(password: String): F[String] = passwordHashingService.hash(password)

  override def login(email: EmailAddress, password: String): F[AuthenticationToken] =
    for {
      databaseUser <- userDao
        .findByEmail(email)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"Email not found: $email")))

      isLockedUser <- lockedUserDao.findLockedUserById(databaseUser.id).isDefined
      _ <- predicate(isLockedUser, LockedUserAccountException(databaseUser.id))

      isSuccess <- passwordHashingService.checkPassword(password, databaseUser.password)

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      currentDateTime = new DateTime(timestamp)

      _ <- if (isSuccess)
        Applicative[F].pure((): Unit)
      else
        Random[F, UUID].value
          .flatMap { uuid =>
            authenticationFailureDao
              .insert(DatabaseAuthenticationFailure(uuid, databaseUser.id, currentDateTime, deleted = false))
          }
          .productR {
            authenticationFailureDao.findByUser(
              databaseUser.id,
              currentDateTime.minus(authenticationConfiguration.bruteForceProtection.rollOverPeriod.toMillis)
            )
          }
          .flatMap { authenticationFailures =>
            if (authenticationFailures.length >= authenticationConfiguration.bruteForceProtection.maximumFailures)
              authenticationSecretGenerator
                .generate(User.fromDatabaseUser(databaseUser))
                .flatMap { unlockCode =>
                  val lockedUser = DatabaseLockedUser(databaseUser.id, currentDateTime, unlockCode, None)

                  lockedUserDao.insert(lockedUser).as(lockedUser)
                }
                .flatMap {
                  lockedUser => emailService.send {
                    Email.unlockUser(User.fromDatabaseUser(databaseUser), lockedUser)
                  }
                }
                .productR {
                  authenticationFailures.traverse { authenticationFailure =>
                    authenticationFailureDao.delete(authenticationFailure.id)
                  }
                }
                .productR {
                  Sync[F].raiseError[Unit] {
                    AuthenticationException(
                      s"User account has been locked due to ${authenticationConfiguration.bruteForceProtection.maximumFailures} incorrect authentication attempts"
                    )
                  }
                } else
              Sync[F].raiseError[Unit] {
                AuthenticationException(
                  s"Invalid credentials. ${authenticationConfiguration.bruteForceProtection.maximumFailures - authenticationFailures.length} incorrect authentication attempts remain before the user account is locked"
                )
              }
          }

      secret <- authenticationSecretGenerator.generate(User.fromDatabaseUser(databaseUser))

      databaseAuthenticationToken <- authenticationTokenDao.createToken {
        DatabaseAuthenticationToken(
          databaseUser.id,
          currentDateTime,
          currentDateTime.plus(authenticationConfiguration.sessionTimeout.toMillis),
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

      isLockedUser <- lockedUserDao.findLockedUserById(databaseUser.id).isDefined
      _ <- predicate(isLockedUser, LockedUserAccountException(databaseUser.id))

      _ <- authenticationTokenDao.extendExpiry(authenticationToken.secret, authenticationConfiguration.sessionTimeout)
    } yield User.fromDatabaseUser(databaseUser)

  override def resetPassword(email: EmailAddress, frontEndUrl: String): F[ResetPasswordToken] =
    for {
      databaseUser <- userDao
        .findByEmail(email)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"$email was not found")))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      secret <- authenticationSecretGenerator.generate(User.fromDatabaseUser(databaseUser))

      databaseResetPasswordToken = DatabaseResetPasswordToken(
        databaseUser.id,
        secret,
        new DateTime(timestamp),
        new DateTime(timestamp + authenticationConfiguration.sessionTimeout.toMillis),
        None
      )

      _ <- resetPasswordTokenDao.insert(databaseResetPasswordToken)

      resetPasswordToken = ResetPasswordToken.fromDatabaseResetPasswordToken(databaseResetPasswordToken)
      _ <- emailService.send(Email.resetPassword(User.fromDatabaseUser(databaseUser), resetPasswordToken, frontEndUrl))

    } yield resetPasswordToken

  override def getResetPasswordToken(userId: UUID, secret: String): F[ResetPasswordToken] =
    resetPasswordTokenDao
      .find(userId, secret)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("Reset password token not found")))
      .map(ResetPasswordToken.fromDatabaseResetPasswordToken)

  override def passwordResetCompleted(userId: UUID, secret: String): F[ResetPasswordToken] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      success <- resetPasswordTokenDao.resetCompleted(userId, secret, new DateTime(timestamp))

      _ <- predicate(!success, ResourceNotFoundException("Reset password token not found"))
      resetPasswordToken <- getResetPasswordToken(userId, secret)
    } yield resetPasswordToken

}

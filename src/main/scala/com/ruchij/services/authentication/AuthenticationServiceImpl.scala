package com.ruchij.services.authentication

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.daos.user.UserDao
import com.ruchij.exceptions.{AuthenticationException, ResourceNotFoundException}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.hashing.PasswordHashingService
import org.joda.time.DateTime

import scala.language.higherKinds

class AuthenticationServiceImpl[F[_]: Sync: Clock](
  passwordHashingService: PasswordHashingService[F, String],
  databaseUserDao: UserDao[F],
  authenticationTokenDao: AuthenticationTokenDao[F],
  authenticationSecretGenerator: AuthenticationSecretGenerator[F],
  authenticationConfiguration: AuthenticationConfiguration
) extends AuthenticationService[F] {

  override def hashPassword(password: String): F[String] = passwordHashingService.hash(password)

  override def login(email: String, password: String): F[AuthenticationToken] =
    for {
      databaseUser <- databaseUserDao.findByEmail(email)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"Email not found: $email")))

      isSuccess <- passwordHashingService.checkPassword(password, databaseUser.password)
      _ <- if (isSuccess) Applicative[F].unit
      else Sync[F].raiseError[Unit](AuthenticationException("Invalid credentials"))

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

  override def authenticate(secret: String): F[User] =
    for {
      authenticationToken <- authenticationTokenDao
        .find(secret)
        .getOrElseF(Sync[F].raiseError(AuthenticationException("Invalid credentials")))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      _ <- if (authenticationToken.expiresAt.isAfter(timestamp)) Applicative[F].unit
      else Sync[F].raiseError[Unit](AuthenticationException("Expired credentials"))

      databaseUser <- databaseUserDao
        .findById(authenticationToken.userId)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("User not found")))

      _ <- authenticationTokenDao.extendExpiry(
        authenticationToken.secret,
        authenticationConfiguration.sessionTimeout
      )
    } yield User.fromDatabaseUser(databaseUser)
}

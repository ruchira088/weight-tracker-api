package com.ruchij.services.authentication

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.AuthenticationConfiguration
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.daos.user.UserDao
import com.ruchij.exceptions.{AuthenticationException, ResourceNotFoundException}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.hashing.PasswordHashingService
import com.ruchij.types.Random
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

  override def login(username: String, password: String): F[AuthenticationToken] =
    for {
      databaseUser <- databaseUserDao
        .findByUsername(username)
        .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"Username not found: $username")))

      isSuccess <- passwordHashingService.checkPassword(password, databaseUser.password)
      _ <- if (isSuccess) Applicative[F].unit
      else Sync[F].raiseError[Unit](AuthenticationException("Invalid credentials"))

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      secret <- authenticationSecretGenerator.generate(User.fromDatabaseUser(databaseUser))

      authenticationToken <- authenticationTokenDao.createToken {
        AuthenticationToken(
          databaseUser.id,
          new DateTime(timestamp + authenticationConfiguration.sessionTimeout.toMillis),
          secret
        )
      }
    } yield authenticationToken

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
package com.ruchij.test.daos

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import cats.Applicative
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.exceptions.ResourceNotFoundException
import com.ruchij.services.authentication.models.AuthenticationToken
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

class InMemoryAuthenticationTokenDao[F[_]: Sync: Clock](
  concurrentHashMap: ConcurrentHashMap[String, AuthenticationToken]
) extends AuthenticationTokenDao[F] {

  override def createToken(authenticationToken: AuthenticationToken): F[AuthenticationToken] =
    Applicative[F]
      .pure(concurrentHashMap.put(authenticationToken.secret, authenticationToken))
      .as(authenticationToken)

  override def find(secret: String): OptionT[F, AuthenticationToken] =
    OptionT.fromOption[F](Option(concurrentHashMap.get(secret)))

  override def extendExpiry(secret: String, duration: FiniteDuration): F[AuthenticationToken] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      authenticationToken <- find(secret).getOrElseF {
        Sync[F].raiseError(ResourceNotFoundException("Unable to find Authentication token"))
      }
      updated <- createToken(authenticationToken.copy(expiresAt = new DateTime(timestamp + duration.toMillis)))
    } yield updated

  override def remove(secret: String): F[AuthenticationToken] =
    Option(concurrentHashMap.remove(secret)).fold[F[AuthenticationToken]](
      Sync[F].raiseError(ResourceNotFoundException("Unable to find Authentication token"))
    ) {
      Applicative[F].pure
    }
}

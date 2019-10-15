package com.ruchij.test.daos

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import cats.Applicative
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.daos.authtokens.AuthenticationTokenDao
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.exceptions.ResourceNotFoundException
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

class InMemoryAuthenticationTokenDao[F[_]: Sync: Clock](
  concurrentHashMap: ConcurrentHashMap[String, DatabaseAuthenticationToken]
) extends AuthenticationTokenDao[F] {

  override def createToken(databaseAuthenticationToken: DatabaseAuthenticationToken): F[DatabaseAuthenticationToken] =
    Applicative[F]
      .pure(concurrentHashMap.put(databaseAuthenticationToken.secret, databaseAuthenticationToken))
      .as(databaseAuthenticationToken)

  override def find(secret: String): OptionT[F, DatabaseAuthenticationToken] =
    OptionT.fromOption[F](Option(concurrentHashMap.get(secret)))

  override def extendExpiry(secret: String, duration: FiniteDuration): F[DatabaseAuthenticationToken] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      databaseAuthenticationToken <- find(secret).getOrElseF {
        Sync[F].raiseError(ResourceNotFoundException("Unable to find Authentication token"))
      }
      updated <- createToken {
        databaseAuthenticationToken.copy(
          expiresAt = new DateTime(timestamp + duration.toMillis),
          renewalCount = databaseAuthenticationToken.renewalCount + 1
        )
      }
    } yield updated

  override def remove(secret: String): F[DatabaseAuthenticationToken] =
    Option(concurrentHashMap.remove(secret)).fold[F[DatabaseAuthenticationToken]](
      Sync[F].raiseError(ResourceNotFoundException("Unable to find Authentication token"))
    ) {
      Applicative[F].pure
    }
}

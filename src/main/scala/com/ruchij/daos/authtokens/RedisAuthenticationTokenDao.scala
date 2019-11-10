package com.ruchij.daos.authtokens

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.RedisConfiguration
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.exceptions.{AuthenticationException, InternalServiceException}
import org.joda.time.DateTime
import redis.{ByteStringFormatter, RedisClient}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

class RedisAuthenticationTokenDao[F[_]: Clock: Sync](redisClient: RedisClient)(
  implicit functionK: Future ~> F,
  byteStringFormatter: ByteStringFormatter[DatabaseAuthenticationToken],
  executionContext: ExecutionContext
) extends AuthenticationTokenDao[F] {

  override def createToken(databaseAuthenticationToken: DatabaseAuthenticationToken): F[DatabaseAuthenticationToken] =
    for {
      _ <-
        Sync[F].suspend {
          functionK(redisClient.set(databaseAuthenticationToken.secret, databaseAuthenticationToken))
        }

      persistedToken <- find(databaseAuthenticationToken.secret)
        .getOrElseF(Sync[F].raiseError(InternalServiceException("Unable to persist authentication token")))

    } yield persistedToken

  override def find(secret: String): OptionT[F, DatabaseAuthenticationToken] =
    OptionT {
      Sync[F].suspend {
        functionK(redisClient.get[DatabaseAuthenticationToken](secret))
      }
    }

  override def extendExpiry(secret: String, duration: FiniteDuration): F[DatabaseAuthenticationToken] =
    for {
      databaseAuthenticationToken <- getToken(secret)

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      _ <-
        Sync[F].suspend {
          functionK {
            redisClient.set(
              secret,
              databaseAuthenticationToken.copy(
                renewalCount = databaseAuthenticationToken.renewalCount + 1,
                expiresAt = new DateTime(timestamp).plus(duration.toMillis)
              )
            )
          }
        }

      updatedAuthenticationToken <- find(secret).getOrElseF(
        Sync[F].raiseError(InternalServiceException("Unable to update token"))
      )
    } yield updatedAuthenticationToken

  override def remove(secret: String): F[DatabaseAuthenticationToken] =
    for {
      databaseAuthToken <- getToken(secret)
      _ <- Sync[F].suspend(functionK(redisClient.del(secret)))
    }
    yield databaseAuthToken

  private def getToken(secret: String): F[DatabaseAuthenticationToken] =
    Sync[F].suspend {
      functionK {
        OptionT(redisClient.get[DatabaseAuthenticationToken](secret))
          .getOrElseF(Future.failed(AuthenticationException("Authentication token not found")))
      }
    }
}

object RedisAuthenticationTokenDao {
  def redisClient(redisConfiguration: RedisConfiguration)(implicit actorSystem: ActorSystem): RedisClient =
    RedisClient(redisConfiguration.host, redisConfiguration.port, redisConfiguration.password)
}

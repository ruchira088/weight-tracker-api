package com.ruchij.daos.authtokens

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.RedisConfiguration
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.exceptions.{AuthenticationException, InternalServiceException}
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import org.joda.time.DateTime
import redis.{ByteStringFormatter, RedisClient}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

class RedisAuthenticationTokenDao[F[_]: Clock: Sync](redisClient: RedisClient)(
  implicit transformation: Future ~> F,
  byteStringFormatter: ByteStringFormatter[DatabaseAuthenticationToken],
  executionContext: ExecutionContext
) extends AuthenticationTokenDao[F] {

  override def createToken(databaseAuthenticationToken: DatabaseAuthenticationToken): F[DatabaseAuthenticationToken] =
    for {
      _ <- Transformation[Future, F].apply {
        redisClient.set(databaseAuthenticationToken.secret, databaseAuthenticationToken)
      }

      persistedToken <- find(databaseAuthenticationToken.secret)
        .getOrElseF(Sync[F].raiseError(InternalServiceException("Unable to persist authentication token")))

    } yield persistedToken

  override def find(secret: String): OptionT[F, DatabaseAuthenticationToken] =
    for {
      databaseAuthenticationToken <- OptionT {
        Transformation[Future, F].apply {
          redisClient.get[DatabaseAuthenticationToken](secret)
        }
      }

      timestamp <- OptionT.liftF(Clock[F].realTime(TimeUnit.MILLISECONDS))

      token <- if (databaseAuthenticationToken.expiresAt.isAfter(timestamp))
        OptionT.pure[F](databaseAuthenticationToken)
      else
        OptionT.liftF[F, DatabaseAuthenticationToken] {
          Sync[F].raiseError(AuthenticationException("Expired authentication token"))
        }
    } yield token

  override def extendExpiry(secret: String, duration: FiniteDuration): F[DatabaseAuthenticationToken] =
    for {
      databaseAuthenticationToken <- getToken(secret)

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      _ <- Transformation[Future, F].apply(
        redisClient.set(
          secret,
          databaseAuthenticationToken.copy(
            renewalCount = databaseAuthenticationToken.renewalCount + 1,
            expiresAt = new DateTime(timestamp).plus(duration.toMillis)
          )
        )
      )

      updatedAuthenticationToken <- find(secret).getOrElseF(
        Sync[F].raiseError(InternalServiceException("Unable to update token"))
      )
    } yield updatedAuthenticationToken

  override def remove(secret: String): F[DatabaseAuthenticationToken] =
    for {
      databaseAuthToken <- getToken(secret)
      _ <- Transformation[Future, F].apply(redisClient.del(secret))
    }
    yield databaseAuthToken

  private def getToken(secret: String): F[DatabaseAuthenticationToken] =
    Transformation[Future, F].apply {
      OptionT(redisClient.get[DatabaseAuthenticationToken](secret))
        .getOrElseF(Future.failed(AuthenticationException("Authentication token not found")))
    }
}

object RedisAuthenticationTokenDao {
  def redisClient(redisConfiguration: RedisConfiguration)(implicit actorSystem: ActorSystem): RedisClient =
    RedisClient(redisConfiguration.host, redisConfiguration.port)
}

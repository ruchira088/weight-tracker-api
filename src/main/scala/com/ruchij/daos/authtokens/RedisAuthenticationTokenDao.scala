package com.ruchij.daos.authtokens

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.RedisConfiguration
import com.ruchij.daos.authtokens.RedisAuthenticationTokenDao.key
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import com.ruchij.exceptions.{AuthenticationException, InternalServiceException}
import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.types.{RandomUuid, Transformation}
import com.ruchij.types.Transformation.~>
import org.joda.time.DateTime
import redis.{ByteStringFormatter, RedisClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

class RedisAuthenticationTokenDao[F[_]: RandomUuid: Clock: Sync](redisClient: RedisClient)(
  implicit transformation: Future ~> F,
  byteStringFormatter: ByteStringFormatter[DatabaseAuthenticationToken],
  executionContext: ExecutionContext
) extends AuthenticationTokenDao[F] {

  override def createToken(authenticationToken: AuthenticationToken): F[AuthenticationToken] =
    for {
      id <- RandomUuid[F].uuid
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      _ <- Transformation[Future, F].apply {
        redisClient.set(
          key(authenticationToken.userId, authenticationToken.secret),
          DatabaseAuthenticationToken(
            id,
            authenticationToken.userId,
            new DateTime(timestamp),
            authenticationToken.expiresAt,
            0,
            authenticationToken.secret,
            None
          )
        )
      }

      persistedToken <- findByUserIdAndSecret(authenticationToken.userId, authenticationToken.secret)
        .getOrElseF(Sync[F].raiseError(InternalServiceException("Unable to persist authentication token")))

    } yield persistedToken

  override def findByUserIdAndSecret(userId: UUID, authenticationSecret: UUID): OptionT[F, AuthenticationToken] =
    OptionT {
      Clock[F]
        .realTime(TimeUnit.MILLISECONDS)
        .flatMap { timestamp =>
          Transformation[Future, F].apply {
            OptionT(redisClient.get[DatabaseAuthenticationToken](key(userId, authenticationSecret))).semiflatMap {
              databaseAuthenticationToken =>
                if (databaseAuthenticationToken.expiresAt.isAfter(timestamp))
                  Future.successful(AuthenticationToken.fromDatabaseAuthenticationToken(databaseAuthenticationToken))
                else
                  Future.failed(AuthenticationException("Expired authentication token"))
            }.value
          }
        }
    }

  override def findByUserId(userId: UUID): F[List[AuthenticationToken]] = ???

  override def extendExpiry(
    userId: UUID,
    authenticationSecret: UUID,
    duration: FiniteDuration
  ): F[AuthenticationToken] = ???

  override def remove(userId: UUID, authenticationSecret: UUID): F[AuthenticationToken] = ???
}

object RedisAuthenticationTokenDao {
  def key(userId: UUID, authenticationSecret: UUID): String = s"$userId-$authenticationSecret"

  def redisClient(redisConfiguration: RedisConfiguration)(implicit actorSystem: ActorSystem): RedisClient =
    RedisClient(redisConfiguration.host, redisConfiguration.port)
}

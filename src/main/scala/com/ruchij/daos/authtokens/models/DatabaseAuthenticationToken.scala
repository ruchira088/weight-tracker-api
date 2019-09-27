package com.ruchij.daos.authtokens.models

import java.util.UUID

import akka.util.ByteString
import com.ruchij.circe.Decoders.jodaTimeDecoder
import com.ruchij.circe.Encoders.jodaTimeEncoder
import com.ruchij.types.UnsafeCopoint
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser.parse
import org.joda.time.DateTime
import redis.ByteStringFormatter

case class DatabaseAuthenticationToken(
  userId: UUID,
  createdAt: DateTime,
  expiresAt: DateTime,
  renewalCount: Long,
  secret: String,
  deletedAt: Option[DateTime]
)

object DatabaseAuthenticationToken {
  implicit val databaseAuthenticationTokenByteStringFormatter: ByteStringFormatter[DatabaseAuthenticationToken] =
    new ByteStringFormatter[DatabaseAuthenticationToken] {
      override def serialize(databaseAuthenticationToken: DatabaseAuthenticationToken): ByteString =
        ByteString(databaseAuthenticationToken.asJson.spaces2)

      override def deserialize(byteString: ByteString): DatabaseAuthenticationToken =
        UnsafeCopoint.unsafeExtract {
          parse(byteString.utf8String).toTry
            .flatMap(_.as[DatabaseAuthenticationToken].toTry)
        }
    }
}

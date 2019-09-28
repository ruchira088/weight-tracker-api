package com.ruchij.web.requests

import cats.effect.Sync
import com.ruchij.circe.Decoders.jodaTimeDecoder
import org.http4s.EntityDecoder
import org.joda.time.DateTime
import org.http4s.circe.jsonOf
import io.circe.generic.auto._

import scala.language.higherKinds

case class CreateWeightEntryRequest(timestamp: DateTime, weight: Double, description: Option[String])

object CreateWeightEntryRequest {
  implicit def createWeightEntryRequest[F[_]: Sync]: EntityDecoder[F, CreateWeightEntryRequest] =
    jsonOf[F, CreateWeightEntryRequest]
}

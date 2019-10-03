package com.ruchij.web.requests.bodies

import cats.effect.Sync
import com.ruchij.circe.Decoders.jodaTimeDecoder
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.joda.time.DateTime

import scala.language.higherKinds

case class CreateWeightEntryRequest(timestamp: DateTime, weight: Double, description: Option[String])

object CreateWeightEntryRequest {
  implicit def createWeightEntryRequest[F[_]: Sync]: EntityDecoder[F, CreateWeightEntryRequest] =
    jsonOf[F, CreateWeightEntryRequest]
}

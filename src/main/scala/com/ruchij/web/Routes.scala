package com.ruchij.web

import cats.implicits._
import cats.effect.{Clock, Sync}
import com.ruchij.web.responses.HealthCheck
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

object Routes {
  def apply[F[_]](implicit sync: Sync[F], clock: Clock[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case GET -> Root / "health" =>
        for {
          timestamp <- clock.realTime(MILLISECONDS)
          response <- Ok { HealthCheck(new DateTime(timestamp)) }
        }
        yield response
    }
  }
}

package com.ruchij.web.routes

import cats.data.OptionT
import cats.{Applicative, Defer}
import com.ruchij.web.assets.ResourceFileService
import com.ruchij.web.routes.Paths.`favicon.ico`
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object StaticAssetRoutes {
  def apply[F[_]: Defer: Applicative](
    resourceFileSource: ResourceFileService[F]
  )(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes[F] {
      case request @ GET -> Root / `favicon.ico` => resourceFileSource.serve(request, `favicon.ico`)

      case _ => OptionT.none
    }
  }
}

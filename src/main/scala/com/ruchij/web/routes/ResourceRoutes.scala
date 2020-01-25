package com.ruchij.web.routes

import cats.data.OptionT
import cats.{Applicative, Defer}
import com.ruchij.services.resource.ResourceService
import com.ruchij.web.assets.StaticResourceService
import com.ruchij.web.routes.Paths.{`favicon.ico`, resources}
import org.http4s.{Headers, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

import scala.language.higherKinds

object ResourceRoutes {
  def apply[F[_]: Defer: Applicative](
    staticResourceService: StaticResourceService[F],
    resourceService: ResourceService[F]
  )(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes[F] {
      case request @ GET -> Root / `favicon.ico` => staticResourceService.serve(request, `favicon.ico`)

      case GET -> `resources` /: key =>
        resourceService.fetchByKey(key.toList.mkString("/"))
          .map {
            resource =>
              Response(body = resource.data, headers = Headers.of(`Content-Type`(resource.contentType)))
          }

      case _ => OptionT.none
    }
  }
}

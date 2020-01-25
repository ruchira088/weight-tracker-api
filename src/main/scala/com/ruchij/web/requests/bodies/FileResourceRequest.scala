package com.ruchij.web.requests.bodies

import cats.Applicative
import cats.effect.Bracket
import com.ruchij.exceptions.ValidationException
import com.ruchij.services.resource.models.Resource
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.{Multipart, Part}

import scala.language.higherKinds

case class FileResourceRequest[F[_]](name: String, fileName: String, resource: Resource[F])

object FileResourceRequest {
  def unapply[F[_]](part: Part[F]): Option[(String, String, Resource[F])] =
    for {
      name <- part.name
      fileName <- part.filename
      contentType <- part.headers.get(`Content-Type`)
    } yield (name, fileName, Resource(contentType.mediaType, part.body))

  def parse[F[_]](multipart: Multipart[F]): Vector[FileResourceRequest[F]] =
    multipart.parts.collect {
      case FileResourceRequest(name, fileName, resource) =>
        FileResourceRequest(name, fileName, resource)
    }

  def parse[F[_], G[_]: Bracket[*[_], Throwable]](key: String, multipart: Multipart[F]): G[FileResourceRequest[F]] =
    parse(multipart)
      .find(_.name == key)
      .fold(
        Bracket[G, Throwable].raiseError[FileResourceRequest[F]](ValidationException(s"Unable to find $key as file value"))
      ) {
        Applicative[G].pure[FileResourceRequest[F]]
      }
}

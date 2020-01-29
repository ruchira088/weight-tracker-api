package com.ruchij.services.resource

import java.nio.file.Path

import cats.data.OptionT
import cats.effect.{Async, Blocker, Concurrent, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.services.resource.FileResourceService.FileResource
import com.ruchij.services.resource.models.Resource
import com.ruchij.circe.Decoders.{mediaTypeCirceDecoder, pathCirceDecoder}
import com.ruchij.circe.Encoders.{mediaTypeCirceEncoder, pathCirceEncoder}
import com.ruchij.utils.FileUtils
import fs2.io.file.{readAll, writeAll}
import fs2.text.{lines, utf8Decode}
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, parser}
import org.http4s.MediaType

import scala.language.higherKinds

class FileResourceService[F[_]: Async: ContextShift: Concurrent](fileResourceFolder: Path, metaDataFile: Path, blocker: Blocker)(
  implicit eitherThrowableToF: Either[Throwable, *] ~> F
) extends ResourceService[F] {

  override type InsertionResult = Path

  override type DeletionResult = Path

  override def insert(key: String, resource: Resource[F]): F[Path] =
    for {
      path <- Sync[F].delay(fileResourceFolder.resolve(key))

      insertMetaData <- Concurrent[F].start {
        FileUtils.append(
          metaDataFile,
          (Codec[FileResource].apply(FileResource(key, resource.contentType, path)).noSpaces + "\n").getBytes
        )
      }

      _ <- blocker.delay(path.toFile.delete())
      _ <- blocker.delay(path.getParent.toFile.mkdirs())
      _ <- blocker.delay(path.toFile.createNewFile())

      _ <- resource.data.through(writeAll(path, blocker)).compile.drain

      _ <- insertMetaData.join
    } yield path

  override def fetch(key: String): OptionT[F, Resource[F]] =
    OptionT {
      readAll(metaDataFile, blocker, 4096)
        .through(utf8Decode)
        .through(lines)
        .evalMap(jsonString => eitherThrowableToF(parser.parse(jsonString)))
        .evalMap(json => eitherThrowableToF(json.as[FileResource]))
        .find(_.key == key)
        .compile
        .toList
        .map {
          _.headOption.map {
            fileResource =>
              Resource(fileResource.mediaType, readAll(fileResource.resourcePath, blocker, 4096))
          }
        }
    }

  override def delete(key: String): OptionT[F, Path] = ???
}

object FileResourceService {
  case class FileResource(key: String, mediaType: MediaType, resourcePath: Path)

  implicit val fileResourceCirceCodec: Codec[FileResource] = deriveCodec[FileResource]
}

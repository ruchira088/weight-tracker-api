package com.ruchij.services.resource

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import cats.data.OptionT
import cats.effect.{Async, Blocker, Clock, Concurrent, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.services.resource.FileResourceService.FileResource
import com.ruchij.services.resource.models.Resource
import com.ruchij.circe.Decoders.{jodaTimeCirceDecoder, mediaTypeCirceDecoder, pathCirceDecoder}
import com.ruchij.circe.Encoders.{jodaTimeCirceEncoder, mediaTypeCirceEncoder, pathCirceEncoder}
import com.ruchij.types.Ordering.jodaDateTimeOrdering
import com.ruchij.utils.FileUtils
import fs2.io.file.{readAll, writeAll, delete => deleteFile, deleteIfExists}
import fs2.text.{lines, utf8Decode}
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Encoder, parser}
import org.http4s.MediaType
import org.joda.time.DateTime

import scala.language.higherKinds

class FileResourceService[F[_]: Async: ContextShift: Concurrent: Clock](
  fileResourceFolder: Path,
  metaDataFile: Path,
  blocker: Blocker
)(implicit eitherThrowableToF: Either[Throwable, *] ~> F)
    extends ResourceService[F] {

  override type InsertionResult = Path

  override type DeletionResult = Path

  override def insert(key: String, resource: Resource[F]): F[Path] =
    for {
      path <- Sync[F].delay(fileResourceFolder.resolve(key))
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      fileResource = FileResource(key, new DateTime(timestamp), resource.contentType, path, deleted = false)

      insertMetaData <- Concurrent[F].start {
        FileUtils.append(metaDataFile, (Encoder[FileResource].apply(fileResource).noSpaces + "\n").getBytes)
      }

      _ <- blocker.delay(deleteIfExists(blocker, path))
      _ <- blocker.delay(path.getParent.toFile.mkdirs())
      _ <- blocker.delay(path.toFile.createNewFile())

      _ <- resource.data.through(writeAll(path, blocker)).compile.drain

      _ <- insertMetaData.join
    } yield path

  override def fetch(key: String): OptionT[F, Resource[F]] =
    find(key)
      .map { fileResource =>
        Resource(fileResource.mediaType, readAll(fileResource.resourcePath, blocker, 4096))
      }

  override def delete(key: String): OptionT[F, Path] =
    find(key)
      .semiflatMap { fileResource =>
        Clock[F]
          .realTime(TimeUnit.MILLISECONDS)
          .flatMap { timestamp =>
            val updatedFileResource = fileResource.copy(timestamp = new DateTime(timestamp), deleted = true)

            Concurrent[F].start {
              FileUtils.append(metaDataFile, (Encoder[FileResource].apply(updatedFileResource).noSpaces + "\n").getBytes)
            }
          }
          .productL(deleteFile(blocker, fileResource.resourcePath))
          .flatMap(_.join)
          .as(fileResource.resourcePath)
      }

  def find(key: String): OptionT[F, FileResource] =
    OptionT {
      readAll(metaDataFile, blocker, 4096)
        .through(utf8Decode)
        .through(lines)
        .filter(_.trim.nonEmpty)
        .evalMap(jsonString => eitherThrowableToF(parser.parse(jsonString)))
        .evalMap(json => eitherThrowableToF(json.as[FileResource]))
        .filter(_.key == key)
        .compile
        .toList
        .map {
          _.sortBy(_.timestamp).lastOption.filter(!_.deleted)
        }
    }
}

object FileResourceService {
  case class FileResource(key: String, timestamp: DateTime, mediaType: MediaType, resourcePath: Path, deleted: Boolean)

  implicit val fileResourceCirceCodec: Codec[FileResource] = deriveCodec[FileResource]
}

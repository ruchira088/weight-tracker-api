package com.ruchij.services.resource

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Show, ~>}
import com.ruchij.services.resource.models.Resource
import fs2.{Chunk, Stream}
import org.http4s.MediaType
import software.amazon.awssdk.core.internal.async.{ByteArrayAsyncRequestBody, ByteArrayAsyncResponseTransformer}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future
import scala.language.higherKinds

class S3ResourceService[F[_]: Sync](s3AsyncClient: S3AsyncClient, s3Bucket: String, prefixKey: String)(
  implicit futureToF: Future ~> F,
  eitherFailureToF: Either[Throwable, *] ~> F
) extends ResourceService[F] {

  override type InsertionResult = CompleteMultipartUploadResponse

  override type DeletionResult = HeadObjectResponse

  override def insert(key: String, resource: Resource[F]): F[CompleteMultipartUploadResponse] =
    createMultipartUpload(prefixKey + key, resource.contentType)
      .flatMap { response =>
        resource.data
          .chunkMin(5 * 1024 * 1024)
          .zipWithIndex
          .evalMap {
            case (chunk, index) => multipartUpload(prefixKey + key, response.uploadId(), (index + 1).toInt, chunk.toArray)
          }
          .compile
          .toList
          .flatMap { completedParts =>
            completeUpload(key, response.uploadId(), completedParts)
          }
      }

  def createMultipartUpload(key: String, mediaType: MediaType): F[CreateMultipartUploadResponse] =
    Sync[F].defer {
      futureToF {
        s3AsyncClient
          .createMultipartUpload(
            CreateMultipartUploadRequest
              .builder()
              .bucket(s3Bucket)
              .key(key)
              .contentType(Show[MediaType].show(mediaType))
              .build()
          )
          .toScala
      }
    }

  def multipartUpload(key: String, uploadId: String, part: Int, data: Array[Byte]): F[CompletedPart] =
    Sync[F]
      .defer {
        futureToF {
          s3AsyncClient
            .uploadPart(
              UploadPartRequest.builder().bucket(s3Bucket).key(key).uploadId(uploadId).partNumber(part).build(),
              new ByteArrayAsyncRequestBody(data)
            )
            .toScala
        }
      }
      .map { uploadedPartResponse =>
        CompletedPart.builder().eTag(uploadedPartResponse.eTag()).partNumber(part).build()
      }

  def completeUpload(
    key: String,
    uploadId: String,
    completedParts: List[CompletedPart]
  ): F[CompleteMultipartUploadResponse] =
    Sync[F].defer {
      futureToF {
        s3AsyncClient.completeMultipartUpload {
          CompleteMultipartUploadRequest
            .builder()
            .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts: _*).build())
            .bucket(s3Bucket)
            .key(key)
            .uploadId(uploadId)
            .build()
        }.toScala
      }
    }

  override def fetch(key: String): OptionT[F, Resource[F]] =
    fetchResourceDetails(prefixKey + key)
      .semiflatMap { headObjectResponse =>
        eitherFailureToF(MediaType.parse(headObjectResponse.contentType()))
          .map { mediaType =>
            val data =
              range(0, headObjectResponse.contentLength(), 64 * 1024)
                .evalMap {
                  case (start, end) => fetchPart(prefixKey + key, start, end)
                }
                .mapChunks(_.flatten)

            Resource(mediaType, data)
          }
      }

  def fetchResourceDetails(key: String): OptionT[F, HeadObjectResponse] =
    OptionT {
      Sync[F].defer {
        futureToF {
          s3AsyncClient.headObject(HeadObjectRequest.builder().bucket(s3Bucket).key(key).build()).toScala
        }
      }
        .map(Option.apply)
        .recoverWith {
          case throwable =>
            Option(throwable.getCause)
              .fold[F[Option[HeadObjectResponse]]](Sync[F].raiseError(throwable)) {
                case _: NoSuchKeyException => Applicative[F].pure(None)
                case cause => Sync[F].raiseError(cause)
              }
        }
    }

  def range(start: Long, end: Long, interval: Long): Stream[F, (Long, Long)] =
    if (start + interval > end)
      Stream(start -> end)
    else
      Stream(start -> (start + interval)) ++ range(start + interval + 1, end, interval)

  def fetchPart(key: String, start: Long, end: Long): F[Chunk[Byte]] =
    Sync[F]
      .defer {
        futureToF {
          s3AsyncClient
            .getObject(
              GetObjectRequest.builder().bucket(s3Bucket).key(key).range(s"bytes=$start-$end").build(),
              new ByteArrayAsyncResponseTransformer[GetObjectResponse]
            )
            .toScala
        }
      }
      .map { responseBytes =>
        Chunk.bytes(responseBytes.asByteArray())
      }

  override def delete(key: String): OptionT[F, HeadObjectResponse] =
    fetchResourceDetails(key)
      .productL {
        OptionT.liftF {
          Sync[F].defer {
            futureToF {
              s3AsyncClient.deleteObject(DeleteObjectRequest.builder().bucket(s3Bucket).key(key).build()).toScala
            }
          }
        }
      }
}

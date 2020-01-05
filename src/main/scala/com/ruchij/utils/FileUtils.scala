package com.ruchij.utils

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Path, StandardOpenOption}

import cats.effect.{Async, Resource, Sync}
import cats.implicits._

import scala.language.higherKinds

object FileUtils {
  def append[F[_]: Async](path: Path, content: Array[Byte]): F[Int] =
    Resource
      .make(Sync[F].delay(AsynchronousFileChannel.open(path, StandardOpenOption.WRITE)))(
        fileChannel => Sync[F].delay(fileChannel.close())
      )
      .use { fileChannel =>
        Sync[F]
          .delay(fileChannel.size())
          .flatMap { size =>
            Async[F].async[Int] { callback =>
              fileChannel.write(
                ByteBuffer.wrap(content),
                size,
                (): Unit,
                new CompletionHandler[Integer, Unit] {
                  override def completed(result: Integer, attachment: Unit): Unit =
                    callback(Right(result))

                  override def failed(throwable: Throwable, attachment: Unit): Unit =
                    callback(Left(throwable))
                }
              )
            }
          }
      }

  def readFile[F[_]: Async](path: Path): F[Array[Byte]] =
    Resource
      .make(Sync[F].delay(AsynchronousFileChannel.open(path, StandardOpenOption.READ))) { fileChannel =>
        Sync[F].delay(fileChannel.close())
      }
      .use { fileChannel =>
        Sync[F]
          .delay(fileChannel.size())
          .flatMap[Int] { fileSize =>
            if (fileSize >= Int.MaxValue)
              Sync[F].raiseError(new IllegalArgumentException(s"File at $path exceeds maximum file size"))
            else
              Sync[F].delay(fileSize.toInt)
          }
          .flatMap { fileSize =>
            Async[F].async { callback =>
              val byteBuffer = ByteBuffer.allocate(fileSize)
              fileChannel.read(
                byteBuffer,
                0,
                (): Unit,
                new CompletionHandler[Integer, Unit] {
                  override def completed(result: Integer, attachment: Unit): Unit =
                    callback(Right(byteBuffer.array()))

                  override def failed(throwable: Throwable, attachment: Unit): Unit =
                    callback(Left(throwable))
                }
              )
            }
          }
      }
}

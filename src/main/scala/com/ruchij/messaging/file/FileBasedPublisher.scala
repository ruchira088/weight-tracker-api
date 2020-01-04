package com.ruchij.messaging.file

import java.nio.file.Path

import cats.effect.Async
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.models.Message
import com.ruchij.utils.FileUtils

import scala.language.higherKinds

class FileBasedPublisher[F[_]: Async](filePath: Path) extends Publisher[F, Int] {
  override def publish[A](message: Message[A]): F[Int] =
    FileUtils.append(
      filePath,
      (Message.messageEncoder(message.topic.codec).apply(message).noSpaces + "\n").getBytes
    )
}

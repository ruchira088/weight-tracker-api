package com.ruchij.web.assets

import cats.data.OptionT
import cats.effect.{Blocker, ContextShift, Sync}
import org.http4s.{Request, Response, StaticFile}

import scala.language.higherKinds

class ResourceFileService[F[_]: Sync: ContextShift](blocker: Blocker) {
  def serve(request: Request[F], name: String): OptionT[F, Response[F]] =
    StaticFile.fromResource("/" + name, blocker, Some(request), preferGzipped = true)
}

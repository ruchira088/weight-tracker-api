package com.ruchij.services.resource.models

import fs2.Stream
import org.http4s.MediaType

import scala.language.higherKinds

case class Resource[F[_]](contentType: MediaType, data: Stream[F, Byte])

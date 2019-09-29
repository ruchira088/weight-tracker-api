package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.daos.weightentry.WeightEntryDao.{PageNumber, PageSize}
import io.circe.Encoder
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class PaginatedResultsResponse[+A](results: List[A], pageNumber: PageNumber, pageSize: PageSize)

object PaginatedResultsResponse {
  implicit val pageNumberEncoder: Encoder[PageNumber] = Encoder[Int].contramap[PageNumber](_.toInt)

  implicit val pageSizeEncoder: Encoder[PageSize] = Encoder[Int].contramap[PageSize](_.toInt)

  implicit def paginatedResultsResponseEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, PaginatedResultsResponse[A]] =
    jsonEncoderOf[F, PaginatedResultsResponse[A]]
}

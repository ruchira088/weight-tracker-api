package com.ruchij.web.requests.queryparameters

import cats.data.ValidatedNel
import com.ruchij.types.Tags
import com.ruchij.types.Tags.{PageNumber, PageSize}
import org.http4s.{ParseFailure, QueryParam, QueryParamDecoder, QueryParameterKey, QueryParameterValue}

trait QueryParameter[A] extends QueryParam[A] with QueryParamDecoder[A] {
  override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, A] =
    queryParamDecoder.decode(value)

  val defaultValue: A

  val queryParamDecoder: QueryParamDecoder[A]
}

object QueryParameter {
  def apply[A](implicit queryParameter: QueryParameter[A]): QueryParameter[A] = queryParameter

  implicit object PageNumberQueryParameter extends QueryParameter[PageNumber] {
    override val defaultValue: PageNumber = Tags.pageNumber(0)

    override def key: QueryParameterKey = QueryParameterKey("page-number")

    override val queryParamDecoder: QueryParamDecoder[PageNumber] =
      QueryParamDecoder[Int].map(Tags.pageNumber)
  }

  implicit object PageSizeQueryParameter extends QueryParameter[PageSize] {
    override val defaultValue: PageSize = Tags.pageSize(10)

    override def key: QueryParameterKey = QueryParameterKey("page-size")

    override val queryParamDecoder: QueryParamDecoder[PageSize] =
      QueryParamDecoder[Int].map(Tags.pageSize)
  }
}

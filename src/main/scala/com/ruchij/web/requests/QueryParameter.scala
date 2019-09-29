package com.ruchij.web.requests

import cats.data.ValidatedNel
import com.ruchij.daos.weightentry.WeightEntryDao.{PageNumber, PageNumberTag, PageSize, PageSizeTag}
import org.http4s.{ParseFailure, QueryParam, QueryParamDecoder, QueryParameterKey, QueryParameterValue}
import shapeless.tag

trait QueryParameter[A] extends QueryParam[A] with QueryParamDecoder[A] {
  override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, A] =
    queryParamDecoder.decode(value)

  val defaultValue: A

  val queryParamDecoder: QueryParamDecoder[A]
}

object QueryParameter {
  def apply[A](implicit queryParameter: QueryParameter[A]): QueryParameter[A] = queryParameter

  implicit object PageNumberQueryParameter extends QueryParameter[PageNumber] {
    override val defaultValue: PageNumber = tag[PageNumberTag][Int](0)

    override def key: QueryParameterKey = QueryParameterKey("page-number")

    override val queryParamDecoder: QueryParamDecoder[PageNumber] =
      QueryParamDecoder[Int].map(tag[PageNumberTag][Int])
  }

  implicit object PageSizeQueryParameter extends QueryParameter[PageSize] {
    override val defaultValue: PageSize = tag[PageSizeTag][Int](10)

    override def key: QueryParameterKey = QueryParameterKey("page-size")

    override val queryParamDecoder: QueryParamDecoder[PageSize] =
      QueryParamDecoder[Int].map(tag[PageSizeTag][Int])
  }
}

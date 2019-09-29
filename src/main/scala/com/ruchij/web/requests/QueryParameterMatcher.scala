package com.ruchij.web.requests

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import com.ruchij.daos.weightentry.WeightEntryDao.{PageNumber, PageSize}
import org.http4s.ParseFailure
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

abstract class QueryParameterMatcher[A: QueryParameter]
    extends ValidatingQueryParamDecoderMatcher[A](QueryParameter[A].key.value) {
  override def unapply(params: Map[String, Seq[String]]): Option[ValidatedNel[ParseFailure, A]] =
    params
      .get(QueryParameter[A].key.value)
      .filter(_.nonEmpty)
      .fold[Option[ValidatedNel[ParseFailure, A]]](Some(Valid(QueryParameter[A].defaultValue))) { _ =>
        super.unapply(params)
      }
}

object QueryParameterMatcher {
  case object PageNumberQueryParameterMatcher extends QueryParameterMatcher[PageNumber]

  case object PageSizeQueryParameterMatcher extends QueryParameterMatcher[PageSize]
}

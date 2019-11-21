package com.ruchij.web.headers

import com.ruchij.web.headers.`X-Correlation-ID`.CorrelationId
import org.http4s.HeaderKey
import org.http4s.util.{CaseInsensitiveString, Writer}
import org.http4s.{Header, ParseResult}
import shapeless.tag.@@
import shapeless.tag

object `X-Correlation-ID` extends HeaderKey.Singleton {
  trait CorrelationIdTag
  type CorrelationId = String @@ CorrelationIdTag

  def from(correlationId: String): `X-Correlation-ID` =
    `X-Correlation-ID`(tag[CorrelationIdTag][String](correlationId))

  override type HeaderT = `X-Correlation-ID`

  override def name: CaseInsensitiveString = CaseInsensitiveString("X-Correlation-ID")

  override def matchHeader(header: Header): Option[`X-Correlation-ID`] =
    if (header.name == name && header.value.trim.nonEmpty) parse(header.value).toOption else None

  override def parse(value: String): ParseResult[`X-Correlation-ID`] =
    Right(`X-Correlation-ID`.from(value))
}

case class `X-Correlation-ID`(correlationId: CorrelationId) extends Header.Parsed {
  override def key: `X-Correlation-ID`.type = `X-Correlation-ID`

  override def renderValue(writer: Writer): writer.type =
    writer << correlationId
}

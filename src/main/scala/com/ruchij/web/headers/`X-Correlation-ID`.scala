package com.ruchij.web.headers

import org.http4s.HeaderKey
import org.http4s.util.{CaseInsensitiveString, Writer}
import org.http4s.{Header, ParseResult}

object `X-Correlation-ID` extends HeaderKey.Singleton {
  override type HeaderT = `X-Correlation-ID`

  override def name: CaseInsensitiveString = CaseInsensitiveString("X-Correlation-ID")

  override def matchHeader(header: Header): Option[`X-Correlation-ID`] =
    if (header.name == name && header.value.trim.nonEmpty) parse(header.value).toOption else None

  override def parse(value: String): ParseResult[`X-Correlation-ID`] = Right(`X-Correlation-ID`(value))
}

case class `X-Correlation-ID`(correlationId: String) extends Header.Parsed {
  override def key: `X-Correlation-ID`.type = `X-Correlation-ID`

  override def renderValue(writer: Writer): writer.type =
    writer << correlationId
}

package com.ruchij.types

import org.joda.time.DateTime

import scala.{Ordering => ScalaOrdering}

object Ordering {

  implicit val jodaDateTimeOrdering: Ordering[DateTime] =
    ScalaOrdering.by[DateTime, Long](_.getMillis)
}

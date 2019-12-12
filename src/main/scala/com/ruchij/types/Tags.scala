package com.ruchij.types

import shapeless.tag
import shapeless.tag.@@

object Tags {
  trait PageNumberTag
  trait PageSizeTag

  type PageNumber = Int @@ PageNumberTag
  type PageSize = Int @@ PageSizeTag

  def pageNumber(value: Int): PageNumber = tag[PageNumberTag][Int](value)

  def pageSize(value: Int): PageSize = tag[PageSizeTag][Int](value)
}

package com.ruchij.types

import shapeless.tag
import shapeless.tag.@@

object Tags {

  trait PageNumberTag
  type PageNumber = Int @@ PageNumberTag

  def pageNumber(value: Int): PageNumber = tag[PageNumberTag][Int](value)

  trait PageSizeTag
  type PageSize = Int @@ PageSizeTag

  def pageSize(value: Int): PageSize = tag[PageSizeTag][Int](value)

  trait EmailAddressTag
  type EmailAddress = String @@ EmailAddressTag

  def emailAddress(email: String): EmailAddress = tag[EmailAddressTag][String](email)
}

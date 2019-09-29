package com.ruchij.exceptions

import cats.data.NonEmptyList

case class AggregatedException[+A <: Throwable](errors: NonEmptyList[A]) extends Exception

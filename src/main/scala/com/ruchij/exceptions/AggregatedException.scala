package com.ruchij.exceptions

case class AggregatedException[+A <: Throwable](errors: List[A]) extends Exception

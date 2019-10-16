package com.ruchij.exceptions

case class AuthorizationException(message: String) extends Exception(message)

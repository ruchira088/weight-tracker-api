package com.ruchij.exceptions

case class DatabaseException(message: String) extends Exception(message)

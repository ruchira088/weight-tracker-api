package com.ruchij.exceptions

case class ResourceConflictException(errorMessage: String) extends Exception(errorMessage)

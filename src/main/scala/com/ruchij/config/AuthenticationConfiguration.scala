package com.ruchij.config

import scala.concurrent.duration.FiniteDuration

case class AuthenticationConfiguration(sessionTimeout: FiniteDuration)

package com.ruchij.config

import com.ruchij.config.AuthenticationConfiguration.BruteForceProtectionConfiguration

import scala.concurrent.duration.FiniteDuration

case class AuthenticationConfiguration(
  sessionTimeout: FiniteDuration,
  bruteForceProtection: BruteForceProtectionConfiguration
)

object AuthenticationConfiguration {
  case class BruteForceProtectionConfiguration(maximumFailures: Int, rollOverPeriod: FiniteDuration)
}

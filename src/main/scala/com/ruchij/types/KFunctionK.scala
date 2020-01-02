package com.ruchij.types

import cats.arrow.FunctionK
import cats.~>

import scala.language.higherKinds

trait KFunctionK[F[_], G[_]] {
  val to: FunctionK[F, G]

  val from: FunctionK[G, F]
}

object KFunctionK {
  type <~>[F[_], G[_]] = KFunctionK[F, G]

  def apply[F[_], G[_]](implicit f: F <~> G): F <~> G = f

  implicit def from[F[_], G[_]](implicit fg: F ~> G, gf: G ~> F): F <~> G =
    new <~>[F, G] {
      override val to: ~>[F, G] = fg
      override val from: ~>[G, F] = gf
    }
}

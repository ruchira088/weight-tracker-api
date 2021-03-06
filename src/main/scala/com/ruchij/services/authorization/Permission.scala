package com.ruchij.services.authorization

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed trait Permission extends EnumEntry {
  val includedPermissions: Set[Permission] = Set.empty
}

object Permission extends Enum[Permission] {
  case object READ extends Permission

  case object WRITE extends Permission {
    override val includedPermissions: Set[Permission] = Set(READ)
  }

  override def values: IndexedSeq[Permission] = findValues

  def allPermissions(permission: Permission): Set[Permission] =
    permission.includedPermissions.flatMap(allPermissions) + permission
}

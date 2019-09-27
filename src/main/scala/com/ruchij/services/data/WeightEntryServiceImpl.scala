package com.ruchij.services.data

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect.Clock
import cats.implicits._
import com.ruchij.daos.weightentry.WeightEntryDao
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.services.data.models.WeightEntry
import com.ruchij.types.RandomUuid
import org.joda.time.DateTime

import scala.language.higherKinds

class WeightEntryServiceImpl[F[_]: Clock: Monad: RandomUuid](weightEntryDao: WeightEntryDao[F])
    extends WeightEntryService[F] {

  override def create(weight: Double, description: Option[String], userId: UUID, createdBy: UUID): F[WeightEntry] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      id <- RandomUuid[F].uuid

      databaseWeightEntry <- weightEntryDao.insert {
        DatabaseWeightEntry(id, 0, new DateTime(timestamp), createdBy, userId, weight, description)
      }
    } yield WeightEntry.fromDatabaseWeightEntry(databaseWeightEntry)

  override def findByUser(userId: UUID): F[List[WeightEntry]] =
    weightEntryDao.findByUser(userId)
      .map(_.map(WeightEntry.fromDatabaseWeightEntry))
}

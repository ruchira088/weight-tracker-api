package com.ruchij.services.data

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.daos.weightentry.WeightEntryDao
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.exceptions.{InternalServiceException, ResourceNotFoundException}
import com.ruchij.services.data.models.WeightEntry
import com.ruchij.types.Random
import com.ruchij.types.Tags.{PageNumber, PageSize}
import org.joda.time.DateTime

import scala.language.higherKinds

class WeightEntryServiceImpl[F[_]: Clock: Sync: Random[*[_], UUID]](weightEntryDao: WeightEntryDao[F])
    extends WeightEntryService[F] {

  override def create(
    timestamp: DateTime,
    weight: Double,
    description: Option[String],
    userId: UUID,
    createdBy: UUID
  ): F[WeightEntry] =
    for {
      currentTimestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      id <- Random[F, UUID].value

      _ <- weightEntryDao.insert {
        DatabaseWeightEntry(id, 0, new DateTime(currentTimestamp), createdBy, userId, timestamp, weight, description)
      }

      weightEntry <- getById(id).adaptError {
        case _: ResourceNotFoundException => InternalServiceException("Unable to persist weight entry")
      }
    } yield weightEntry

  override def getById(id: UUID): F[WeightEntry] =
    weightEntryDao
      .findById(id)
      .map(WeightEntry.fromDatabaseWeightEntry)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"Weight entry not found for id = $id")))

  override def findByUser(userId: UUID, pageNumber: PageNumber, pageSize: PageSize): F[List[WeightEntry]] =
    weightEntryDao
      .findByUser(userId, pageNumber, pageSize)
      .map(_.map(WeightEntry.fromDatabaseWeightEntry))

  override def delete(id: UUID): F[WeightEntry] =
    getById(id)
      .flatMap { weightEntry =>
        weightEntryDao.delete(id).flatMap { deleted =>
          if (deleted)
            Applicative[F].pure(weightEntry)
          else
            Sync[F].raiseError(InternalServiceException(s"Unable to delete weight entry with id = $id"))
        }
      }
}

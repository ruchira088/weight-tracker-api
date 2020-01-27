package com.ruchij.services.user

import java.util.UUID

import cats.Applicative
import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.daos.lockeduser.LockedUserDao
import com.ruchij.daos.user.UserDao
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.{InternalServiceException, ResourceConflictException, ResourceNotFoundException}
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.models.Message
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.resource.ResourceService
import com.ruchij.services.resource.models.Resource
import com.ruchij.services.user.models.User
import com.ruchij.types.Random
import com.ruchij.types.Tags.EmailAddress
import com.ruchij.types.Utils.predicate
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class UserServiceImpl[F[_]: Sync: Clock: Random[*[_], UUID]](
  databaseUserDao: UserDao[F],
  lockedUserDao: LockedUserDao[F],
  authenticationService: AuthenticationService[F],
  publisher: Publisher[F, _],
  resourceService: ResourceService[F]
) extends UserService[F] {

  override def create(email: EmailAddress, password: String, firstName: String, lastName: Option[String]): F[User] =
    for {
      emailExists <- findByEmail(email).isDefined
      _ <- predicate(emailExists, ResourceConflictException(s"email already exists: $email"))

      timestamp <- Clock[F].realTime(MILLISECONDS)
      id <- Random[F, UUID].value

      _ <- databaseUserDao.insert {
        DatabaseUser(id, new DateTime(timestamp), new DateTime(timestamp), email, firstName, lastName, None)
      }

      _ <- authenticationService.setPassword(id, password)

      user <- getById(id).adaptError {
        case _: ResourceNotFoundException => InternalServiceException("Unable to persist user")
      }

      _ <- publisher.publish(Message(user))
    } yield user

  override def getById(id: UUID): F[User] =
    databaseUserDao
      .findById(id)
      .map(User.fromDatabaseUser)
      .getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"User not found for id = $id")))

  override def findByEmail(email: EmailAddress): OptionT[F, User] =
    databaseUserDao.findByEmail(email).map(User.fromDatabaseUser)

  override def unlockUser(userId: UUID, unlockCode: String): F[User] =
    for {
      databaseUser <- databaseUserDao.findById(userId).getOrElseF(Sync[F].raiseError(ResourceNotFoundException(s"User not found for id = $userId")))

      success <- lockedUserDao.unlockUser(userId, unlockCode)

      _ <- predicate(!success, ResourceNotFoundException(s"Locked user not found with id = $userId"))
    }
    yield User.fromDatabaseUser(databaseUser)

  override def deleteById(id: UUID): F[User] =
    getById(id).flatMap {
      user =>
        databaseUserDao.deleteById(id).flatMap {
          deleted =>
            if (deleted)
              Applicative[F].pure(user)
            else
              Sync[F].raiseError(InternalServiceException(s"Unable to delete user with id = $id"))
        }
    }

  override def setProfileImage(userId: UUID, fileName: String, image: Resource[F]): F[User] = {
    val key = s"$userId/$fileName"

    resourceService.insert(key, image)
      .productR(databaseUserDao.updateProfileImage(userId, key))
      .productR(getById(userId))
  }
}

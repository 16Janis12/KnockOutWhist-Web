package logic.user.impl

import com.typesafe.config.Config
import logic.user.UserManager
import model.users.User
import util.UserHash

import javax.inject.{Inject, Singleton}

@Singleton
class StubUserManager @Inject()(val config: Config) extends UserManager {
  
  private val user: Map[String, User] = Map(
    "Janis" -> User(
      internalId = 1L,
      id = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      name = "Janis",
      passwordHash = UserHash.hashPW("password123")
    ),
    "Leon" -> User(
      internalId = 2L,
      id = java.util.UUID.fromString("223e4567-e89b-12d3-a456-426614174000"),
      name = "Leon",
      passwordHash = UserHash.hashPW("password123")
    ),
    "Jakob" -> User(
      internalId = 2L,
      id = java.util.UUID.fromString("323e4567-e89b-12d3-a456-426614174000"),
      name = "Jakob",
      passwordHash = UserHash.hashPW("password123")
    )
  )

  override def addUser(name: String, password: String): Boolean = {
    throw new NotImplementedError("StubUserManager.addUser is not implemented")
  }

  override def authenticate(name: String, password: String): Option[User] = {
    user.get(name) match {
      case Some(u) if UserHash.verifyUser(password, u) => Some(u)
      case _ => None
    }
  }

  override def userExists(name: String): Option[User] = {
    user.get(name)
  }

  override def userExistsById(id: Long): Option[User] = {
    user.values.find(_.internalId == id)
  }

  override def removeUser(name: String): Boolean = {
    throw new NotImplementedError("StubUserManager.removeUser is not implemented")
  }
  
}

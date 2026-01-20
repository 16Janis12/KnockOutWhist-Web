package logic.user.impl

import com.typesafe.config.Config
import logic.user.UserManager
import model.users.User
import services.OpenIDUserInfo
import util.UserHash

import javax.inject.{Inject, Singleton}
import scala.collection.mutable

@Singleton
class StubUserManager @Inject()(config: Config) extends UserManager {

  private val user: mutable.Map[String, User] = mutable.Map(
    "Janis" -> User(
      internalId = 1L,
      id = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      name = "Janis",
      passwordHash = UserHash.hashPW("password123")
    ),
    "Leon" -> User(
      internalId = 2L,
      id = java.util.UUID.randomUUID(),
      name = "Jakob",
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
    val newUser = User(
      internalId = user.size.toLong + 1,
      id = java.util.UUID.randomUUID(),
      name = name,
      passwordHash = UserHash.hashPW(password)
    )
    user(name) = newUser
    true
  }

  override def addOpenIDUser(name: String, userInfo: OpenIDUserInfo): Boolean = {
    // For stub implementation, just add a user without password
    val newUser = User(
      internalId = user.size.toLong + 1,
      id = java.util.UUID.randomUUID(),
      name = name,
      passwordHash = "" // No password for OpenID users
    )
    user(name) = newUser
    true
  }

  override def authenticate(name: String, password: String): Option[User] = {
    user.get(name) match {
      case Some(u) if UserHash.verifyUser(password, u) => Some(u)
      case _ => None
    }
  }

  override def authenticateOpenID(provider: String, providerId: String): Option[User] = {
    user.values.find { u =>
      // In a real implementation, this would check stored OpenID provider info
      u.name.startsWith(s"${provider}_") && u.name.contains(providerId)
    }
  }

  override def userExists(name: String): Option[User] = {
    user.get(name)
  }

  override def userExistsById(id: Long): Option[User] = {
    user.values.find(_.internalId == id)
  }

  override def removeUser(name: String): Boolean = {
    user.remove(name).isDefined
  }
}

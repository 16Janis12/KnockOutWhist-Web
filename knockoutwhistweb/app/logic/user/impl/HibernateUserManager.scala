package logic.user.impl

import com.typesafe.config.Config
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import logic.user.UserManager
import model.users.{User, UserEntity}
import play.api.Logger
import services.OpenIDUserInfo
import util.UserHash

import javax.inject.Singleton
import scala.jdk.CollectionConverters.*

@Singleton
class HibernateUserManager @Inject()(em: EntityManager, config: Config) extends UserManager {

  private val logger = Logger(getClass.getName)

  override def addUser(name: String, password: String): Boolean = {
    try {
      // Check if user already exists
      val existing = em.createQuery("SELECT u FROM UserEntity u WHERE u.username = :username", classOf[UserEntity])
        .setParameter("username", name)
        .getResultList
      
      if (!existing.isEmpty) {
        logger.warn(s"User $name already exists")
        return false
      }

      // Create new user
      val userEntity = UserEntity.fromUser(User(
        internalId = 0L, // Will be set by database
        id = java.util.UUID.randomUUID(),
        name = name,
        passwordHash = UserHash.hashPW(password)
      ))

      em.persist(userEntity)
      em.flush()

      true
    } catch {
      case e: Exception => {
        logger.error(s"Error adding user $name", e)
        false
      }
    }
  }

  override def addOpenIDUser(name: String, userInfo: OpenIDUserInfo): Boolean = {
    try {
      // Check if user already exists
      val existing = em.createQuery("SELECT u FROM UserEntity u WHERE u.username = :username", classOf[UserEntity])
        .setParameter("username", name)
        .getResultList
      
      if (!existing.isEmpty) {
        logger.warn(s"User $name already exists")
        return false
      }

      // Check if OpenID user already exists
      val existingOpenID = em.createQuery(
        "SELECT u FROM UserEntity u WHERE u.openidProvider = :provider AND u.openidProviderId = :providerId", 
        classOf[UserEntity]
      )
        .setParameter("provider", userInfo.provider)
        .setParameter("providerId", userInfo.id)
        .getResultList
      
      if (!existingOpenID.isEmpty) {
        logger.warn(s"OpenID user ${userInfo.provider}_${userInfo.id} already exists")
        return false
      }

      // Create new OpenID user
      val userEntity = UserEntity.fromOpenIDUser(name, userInfo)

      em.persist(userEntity)
      em.flush()
      true
    } catch {
      case e: Exception => {
        logger.error(s"Error adding OpenID user ${userInfo.provider}_${userInfo.id}", e)
        false
      }
    }
  }

  override def authenticate(name: String, password: String): Option[User] = {
    try {
      val users = em.createQuery("SELECT u FROM UserEntity u WHERE u.username = :username", classOf[UserEntity])
        .setParameter("username", name)
        .getResultList
      
      if (users.isEmpty) {
        return None
      }

      val userEntity = users.get(0)
      if (UserHash.verifyUser(password, userEntity.toUser)) {
        Some(userEntity.toUser)
      } else {
        None
      }
    } catch {
      case e: Exception => {
        logger.error(s"Error authenticating user $name", e)
        None
      }
    }
  }

  override def authenticateOpenID(provider: String, providerId: String): Option[User] = {
    try {
      val users = em.createQuery(
        "SELECT u FROM UserEntity u WHERE u.openidProvider = :provider AND u.openidProviderId = :providerId", 
        classOf[UserEntity]
      )
        .setParameter("provider", provider)
        .setParameter("providerId", providerId)
        .getResultList
      
      if (users.isEmpty) {
        None
      } else {
        Some(users.get(0).toUser)
      }
    } catch {
      case e: Exception => {
        logger.error(s"Error authenticating OpenID user ${provider}_$providerId", e)
        None
      }
    }
  }

  override def userExists(name: String): Option[User] = {
    try {
      val users = em.createQuery("SELECT u FROM UserEntity u WHERE u.username = :username", classOf[UserEntity])
        .setParameter("username", name)
        .getResultList
      
      if (users.isEmpty) {
        None
      } else {
        Some(users.get(0).toUser)
      }
    } catch {
      case e: Exception => {
        logger.error(s"Error checking if user $name exists", e)
        None
      }
    }
  }

  override def userExistsById(id: Long): Option[User] = {
    try {
      Option(em.find(classOf[UserEntity], id)).map(_.toUser)
    } catch {
      case e: Exception => {
        logger.error(s"Error checking if user with ID $id exists", e)
        None
      }
    }
  }

  override def removeUser(name: String): Boolean = {
    try {
      val users = em.createQuery("SELECT u FROM UserEntity u WHERE u.username = :username", classOf[UserEntity])
        .setParameter("username", name)
        .getResultList
      
      if (users.isEmpty) {
        false
      } else {
        em.remove(users.get(0))
        em.flush()
        true
      }
    } catch {
      case _: Exception => false
    }
  }
}

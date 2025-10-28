package logic.user.impl

import com.typesafe.config.Config
import logic.user.SessionManager
import model.users.User

import javax.inject.{Inject, Singleton}

@Singleton
class BaseSessionManager @Inject()(val config: Config) extends SessionManager {
  
  override def createSession(user: User): String = {
    //TODO create JWT token instead of random string
    //Write session identifier to cache and DB
    val sessionId = java.util.UUID.randomUUID().toString
    sessionId
  }

  override def getUserBySession(sessionId: String): Option[User] = {
    //TODO verify JWT token instead of looking up in cache
    //Read session identifier from cache and DB
    None
  }

  override def invalidateSession(sessionId: String): Unit = {
    
  }
}

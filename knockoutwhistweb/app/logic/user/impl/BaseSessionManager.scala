package logic.user.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.Config
import logic.user.SessionManager
import model.users.User
import services.JwtKeyProvider

import javax.inject.{Inject, Singleton}

@Singleton
class BaseSessionManager @Inject()(val keyProvider: JwtKeyProvider, val config: Config) extends SessionManager {

  private val algorithm = Algorithm.RSA512(keyProvider.publicKey, keyProvider.privateKey)
  
  override def createSession(user: User): String = {
    //Write session identifier to cache and DB
    val sessionId = JWT.create()
      .withIssuer(config.getString("auth.issuer"))
      .withAudience(config.getString("auth.audience"))
      .withSubject(user.internalId.toString)
      .sign(algorithm)
    //TODO write to DB
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

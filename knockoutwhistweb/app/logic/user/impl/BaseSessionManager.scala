package logic.user.impl

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.{JWT, JWTVerifier}
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.typesafe.config.Config
import logic.user.SessionManager
import model.users.User
import scalafx.util.Duration
import services.JwtKeyProvider

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class BaseSessionManager @Inject()(val keyProvider: JwtKeyProvider, val userManager: StubUserManager, val config: Config) extends SessionManager {

  private val algorithm = Algorithm.RSA512(keyProvider.publicKey, keyProvider.privateKey)
  private val verifier: JWTVerifier = JWT.require(algorithm)
    .withIssuer(config.getString("auth.issuer"))
    .withAudience(config.getString("auth.audience"))
    .build()

  //TODO reduce cache to a minimum amount, as JWT should be self-contained
  private val cache: Cache[String, User] = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(5, TimeUnit.MINUTES).build()

  override def createSession(user: User): String = {
    //Write session identifier to cache and DB
    val sessionId = JWT.create()
      .withIssuer(config.getString("auth.issuer"))
      .withAudience(config.getString("auth.audience"))
      .withSubject(user.id.toString)
      .withClaim("id", user.internalId)
      .withExpiresAt(Instant.now.plus(7, ChronoUnit.DAYS))
      .sign(algorithm)
    //TODO write to Redis and DB
    cache.put(sessionId, user)

    sessionId
  }

  override def getUserBySession(sessionId: String): Option[User] = {
    val cachedUser = cache.getIfPresent(sessionId)
    if (cachedUser != null) {
      Some(cachedUser)
    } else {
      val result = Try {
        val decoded = verifier.verify(sessionId)
        val user = userManager.userExistsById(decoded.getClaim("id").asLong())
        user.foreach(u => cache.put(sessionId, u))
        user
      }
      if (result.isSuccess) {
        result.get
      } else {
        None
      }
    }
  }

  override def invalidateSession(sessionId: String): Unit = {
    //TODO remove from Redis and DB
    cache.invalidate(sessionId)
  }
}

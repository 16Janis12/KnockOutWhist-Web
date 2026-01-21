package services

import org.redisson.Redisson
import org.redisson.api.RMapCache
import org.redisson.config.Config
import play.api.Logger
import play.api.libs.json.Json

import java.util.concurrent.TimeUnit
import javax.inject.*

@Singleton
class OAuthCacheService @Inject()() {
  
  private val logger = Logger(this.getClass)
  
  // Initialize Redis connection similar to Gateway
  private val redis = {
    val config: Config = Config()
    val url = "redis://" + sys.env.getOrElse("REDIS_HOST", "localhost") + ":" + sys.env.getOrElse("REDIS_PORT", "6379")
    logger.info(s"OAuthCacheService connecting to Redis at $url")
    config.useSingleServer.setAddress(url)
    Redisson.create(config)
  }
  
  // Cache for OAuth data with 30 minute TTL
  private val oauthCache: RMapCache[String, String] = redis.getMapCache("oauth_cache")
  
  /**
   * Store OAuth data with random ID and return the ID
   */
  def storeOAuthData(userInfo: OpenIDUserInfo, accessToken: String, provider: String): String = {
    val sessionId = java.util.UUID.randomUUID().toString
    
    val oauthData = Json.obj(
      "userInfo" -> Json.toJson(userInfo),
      "accessToken" -> accessToken,
      "provider" -> provider,
      "timestamp" -> System.currentTimeMillis()
    ).toString()
    
    // Store with 30 minute TTL
    oauthCache.put(sessionId, oauthData, 30, TimeUnit.MINUTES)
    logger.info(s"Stored OAuth data for session $sessionId")
    
    sessionId
  }
  
  /**
   * Retrieve OAuth data by session ID
   */
  def getOAuthData(sessionId: String): Option[(OpenIDUserInfo, String, String)] = {
    Option(oauthCache.get(sessionId)) match {
      case Some(dataJson) =>
        try {
          val json = Json.parse(dataJson)
          val userInfo = (json \ "userInfo").as[OpenIDUserInfo]
          val accessToken = (json \ "accessToken").as[String]
          val provider = (json \ "provider").as[String]
          
          logger.info(s"Retrieved OAuth data for session $sessionId")
          Some((userInfo, accessToken, provider))
        } catch {
          case e: Exception =>
            logger.error(s"Failed to parse OAuth data for session $sessionId: ${e.getMessage}")
            None
        }
      case None =>
        logger.warn(s"No OAuth data found for session $sessionId")
        None
    }
  }
  
  /**
   * Remove OAuth data after use
   */
  def removeOAuthData(sessionId: String): Unit = {
    oauthCache.remove(sessionId)
    logger.info(s"Removed OAuth data for session $sessionId")
  }
  
  /**
   * Clean up expired sessions (optional maintenance)
   */
  def cleanupExpiredSessions(): Unit = {
    oauthCache.clear()
    logger.info("Cleaned up expired OAuth sessions")
  }
}

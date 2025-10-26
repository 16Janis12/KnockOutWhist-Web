package logic.user

import model.users.User

trait SessionManager {
  
  def createSession(user: User): String
  def getUserBySession(sessionId: String): Option[User]
  def invalidateSession(sessionId: String): Unit

}

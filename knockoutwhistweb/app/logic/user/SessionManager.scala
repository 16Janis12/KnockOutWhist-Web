package logic.user

import com.google.inject.ImplementedBy
import logic.user.impl.BaseSessionManager
import model.users.User

@ImplementedBy(classOf[BaseSessionManager])
trait SessionManager {

  def createSession(user: User): String

  def getUserBySession(sessionId: String): Option[User]

  def invalidateSession(sessionId: String): Unit

}

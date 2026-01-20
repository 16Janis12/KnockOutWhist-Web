package logic.user

import com.google.inject.ImplementedBy
import logic.user.impl.StubUserManager
import model.users.User
import services.OpenIDUserInfo

@ImplementedBy(classOf[StubUserManager])
trait UserManager {

  def addUser(name: String, password: String): Boolean

  def addOpenIDUser(name: String, userInfo: OpenIDUserInfo): Boolean

  def authenticate(name: String, password: String): Option[User]

  def authenticateOpenID(provider: String, providerId: String): Option[User]

  def userExists(name: String): Option[User]

  def userExistsById(id: Long): Option[User]

  def removeUser(name: String): Boolean

}

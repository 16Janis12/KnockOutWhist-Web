package logic.user

import com.google.inject.ImplementedBy
import logic.user.impl.StubUserManager
import model.users.User

@ImplementedBy(classOf[StubUserManager])
trait UserManager {

  def addUser(name: String, password: String): Boolean
  def authenticate(name: String, password: String): Option[User]
  def userExists(name: String): Option[User]
  def userExistsById(id: Long): Option[User]
  def removeUser(name: String): Boolean

}

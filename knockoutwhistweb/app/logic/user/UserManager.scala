package logic.user

trait UserManager {

  def addUser(name: String, password: String): Boolean
  def authenticate(name: String, password: String): Boolean
  def userExists(name: String): Boolean
  def removeUser(name: String): Boolean

}

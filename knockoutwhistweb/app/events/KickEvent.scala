package events

import model.users.User

case class KickEvent(user: User) extends UserEvent(user) {
  
  override def id: String = "KickEvent"

}

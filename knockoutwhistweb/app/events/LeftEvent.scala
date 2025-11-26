package events

import model.users.User

case class LeftEvent(user: User) extends UserEvent(user) {

  override def id: String = "LeftEvent"
  
}

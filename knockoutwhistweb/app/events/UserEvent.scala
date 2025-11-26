package events

import de.knockoutwhist.utils.events.SimpleEvent
import model.users.User

import java.util.UUID

abstract class UserEvent(user: User) extends SimpleEvent {

  def userId: UUID = user.id
  
}

package model.sessions

import de.knockoutwhist.events.player.{RequestCardEvent, RequestTieChoiceEvent, RequestTrumpSuitEvent}
import de.knockoutwhist.utils.events.SimpleEvent
import model.users.User

import java.util.UUID

class UserSession(user: User, val host: Boolean) extends PlayerSession {
  var canInteract: Boolean = false

  override def updatePlayer(event: SimpleEvent): Unit = {
    event match {
      case event: RequestTrumpSuitEvent =>
        canInteract = true
      case event: RequestTieChoiceEvent =>
        canInteract = true
      case event: RequestCardEvent =>
        canInteract = true
      case _ =>
    }
  }

  override def id: UUID = user.id

  override def name: String = user.name
  
}

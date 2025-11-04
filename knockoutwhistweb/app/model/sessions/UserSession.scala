package model.sessions

import de.knockoutwhist.events.player.{RequestCardEvent, RequestTieChoiceEvent, RequestTrumpSuitEvent}
import de.knockoutwhist.utils.events.SimpleEvent
import model.users.User

import java.util.UUID
import java.util.concurrent.locks.{Lock, ReentrantLock}

class UserSession(user: User, val host: Boolean) extends PlayerSession {
  var canInteract: Option[InteractionType] = None
  val lock: Lock = ReentrantLock()

  override def updatePlayer(event: SimpleEvent): Unit = {
    event match {
      case event: RequestTrumpSuitEvent =>
        canInteract = Some(InteractionType.TrumpSuit)
      case event: RequestTieChoiceEvent =>
        canInteract = Some(InteractionType.TieChoice)
      case event: RequestCardEvent =>
        if (event.player.isInDogLife) canInteract = Some(InteractionType.DogCard)
        else canInteract = Some(InteractionType.Card)
      case _ =>
    }
  }

  override def id: UUID = user.id

  override def name: String = user.name
  
  def resetCanInteract(): Unit = {
    canInteract = None
  }
  
}

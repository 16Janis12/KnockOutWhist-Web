package model.sessions

import de.knockoutwhist.events.player.{RequestCardEvent, RequestTieChoiceEvent, RequestTrumpSuitEvent}
import de.knockoutwhist.utils.events.SimpleEvent
import logic.game.GameLobby
import model.users.User
import play.api.libs.json.JsObject

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import scala.util.Try

class UserSession(val user: User, val host: Boolean, val gameLobby: GameLobby) extends PlayerSession {
  val lock: ReentrantLock = ReentrantLock()
  var canInteract: Option[InteractionType] = None
  var websocketActor: Option[UserWebsocketActor] = None

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
    websocketActor.foreach(_.transmitEventToClient(event))
  }

  override def id: UUID = user.id

  override def name: String = user.name

  def resetCanInteract(): Unit = {
    canInteract = None
  }

  def handleWebResponse(eventType: String, data: JsObject): Unit = {
    lock.lock()
    Try {
      eventType match {
        case "Ping" =>
          // No action needed for Ping
          ()
      }
    }
    lock.unlock()
  }

}

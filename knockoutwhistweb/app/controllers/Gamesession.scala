package controllers

import controllers.sessions.{AdvancedSession, PlayerSession, SimpleSession}
import de.knockoutwhist.cards.{Card, CardValue, Hand, Suit}
import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.control.controllerBaseImpl.BaseGameLogic
import de.knockoutwhist.events.*
import de.knockoutwhist.events.player.{PlayCardEvent, PlayerEvent, ReceivedHandEvent, RequestTieChoiceEvent, RequestTrumpSuitEvent}
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.rounds.Match
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.CustomThread
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

import java.util.UUID
import scala.collection.mutable

class Gamesession(id: UUID) extends CustomThread with EventListener with UI {

  setName("Gamesession")
  private val sessions: mutable.Map[UUID, PlayerSession] = mutable.Map()
  var init = false
  var logic: Option[GameLogic] = None
  override def instance: CustomThread = this

  override def listen(event: SimpleEvent): Unit = {
    runLater {
      event match {
        case event: PlayCardEvent => 
          PodGameManager.transmit(event.player.id, event)
        case event: ReceivedHandEvent => 
          PodGameManager.transmit(event.player.id, event)
        case event: RequestTieChoiceEvent =>
          PodGameManager.transmit(event.player.id, event)
        case event: RequestTrumpSuitEvent =>
          PodGameManager.transmit(event.player.id, event)
        case _ => 
          PodGameManager.transmitAll(event)
      }
      }
  }

  override def initial(gameLogic: GameLogic): Boolean = {
    if (init) {
      return false
    }
    this.logic = Some(gameLogic)
    init = true
    start()
    true
  }
  
  def addSession(session: PlayerSession): Unit = {
    sessions.put(session.id, session)
  }
  
}

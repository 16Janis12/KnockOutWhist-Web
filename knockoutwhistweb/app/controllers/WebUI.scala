package controllers

import controllers.sessions.SimpleSession
import de.knockoutwhist.cards.{Card, CardValue, Hand, Suit}
import de.knockoutwhist.control.GameState.MainMenu
import de.knockoutwhist.events.*
import de.knockoutwhist.events.global.GameStateChangeEvent
import de.knockoutwhist.events.player.PlayerEvent
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.rounds.Match
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.CustomThread
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

object WebUI extends CustomThread with EventListener with UI {

  setName("WebUI")
  
  var init = false

  var latestOutput: String = ""

  override def instance: CustomThread = WebUI

  override def listen(event: SimpleEvent): Unit = {
    runLater {
      event match {
        case event: PlayerEvent =>
          PodGameManager.transmit(event.playerId, event)
        case event: GameStateChangeEvent =>
          if (event.newState == MainMenu) {
            PodGameManager.clearSessions()
          }
          Some(true)
        case _ => 
          PodGameManager.transmitAll(event)
      }
    }
  }

  override def initial: Boolean = {
    if (init) {
      return false
    }
    init = true
    start()
    true
  }
  
}

package controllers

import controllers.sessions.AdvancedSession
import de.knockoutwhist.cards.{Card, CardValue, Hand, Suit}
import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.control.GameState.{InGame, Lobby}
import de.knockoutwhist.control.controllerBaseImpl.BaseGameLogic
import de.knockoutwhist.events.*
import de.knockoutwhist.events.global.GameStateChangeEvent
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.rounds.Match
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.CustomThread
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

object WebUI extends CustomThread with EventListener with UI {

  setName("WebUI")
  
  var init = false
  var logic: Option[GameLogic] = None

  var latestOutput: String = ""

  override def instance: CustomThread = WebUI

  override def listen(event: SimpleEvent): Unit = {
    event match {
      case event: GameStateChangeEvent =>
        if (event.oldState == Lobby && event.newState == InGame) {
          val match1: Option[Match] = logic.get.asInstanceOf[BaseGameLogic].getCurrentMatch
          val players: List[AbstractPlayer] = match1.get.totalplayers
          players.map(player => PodGameManager.addSession(AdvancedSession(player.id, player)))
        }
      case _ =>
    }
  }

  override def initial(gameLogic: GameLogic): Boolean = {
    if (init) {
      return false
    }
    init = true
    this.logic = Some(gameLogic)
    start()
    true
  }
  
}

package controllers

import controllers.sessions.SimpleSession
import de.knockoutwhist.cards.{Card, CardValue, Hand, Suit}
import de.knockoutwhist.events.*
import de.knockoutwhist.events.ERROR_STATUS.*
import de.knockoutwhist.events.GLOBAL_STATUS.*
import de.knockoutwhist.events.PLAYER_STATUS.*
import de.knockoutwhist.events.ROUND_STATUS.{PLAYERS_OUT, SHOW_START_ROUND, WON_ROUND}
import de.knockoutwhist.events.cards.{RenderHandEvent, ShowTieCardsEvent}
import de.knockoutwhist.events.round.ShowCurrentTrickEvent
import de.knockoutwhist.events.ui.GameState.{INGAME, MAIN_MENU}
import de.knockoutwhist.events.ui.{GameState, GameStateUpdateEvent}
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.rounds.Match
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.CustomThread
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

object WebUI extends CustomThread with EventListener with UI {

  setName("WebUI")
  
  var init = false
  private var internState: GameState = GameState.NO_SET

  var latestOutput: String = ""

  override def instance: CustomThread = WebUI

  override def listen(event: SimpleEvent): Unit = {
    runLater {
      event match {
        case event: RenderHandEvent =>
          PodGameManager.transmit(event.player.id, event)
        case event: ShowTieCardsEvent =>
          PodGameManager.transmitAll(event)
        case event: ShowGlobalStatus =>
          if (event.status == TECHNICAL_MATCH_STARTED) {
            val matchImpl = event.objects.head.asInstanceOf[Match]
            for (player <- matchImpl.totalplayers) {
              PodGameManager.addSession(SimpleSession(player.id, ""))
            }
          } else {
            PodGameManager.transmitAll(event)
          }
        case event: ShowPlayerStatus =>
          PodGameManager.transmit(event.player.id, event)
        case event: ShowRoundStatus =>
          PodGameManager.transmitAll(event)
        case event: ShowErrorStatus =>
          PodGameManager.transmitAll(event)
        case event: ShowCurrentTrickEvent =>
          PodGameManager.transmitAll(event)
        case event: GameStateUpdateEvent =>
          if (internState != event.gameState) {
            internState = event.gameState
            if (event.gameState == MAIN_MENU) {
              PodGameManager.clearSessions()
            }
            Some(true)
          }
        case _ => None
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

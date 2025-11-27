package util.mapper

import de.knockoutwhist.events.global.TrickEndEvent
import de.knockoutwhist.rounds.Trick
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object TrickEndEventMapper extends SimpleEventMapper[TrickEndEvent]{
  override def id: String = "TrickEndEvent"

  override def toJson(event: TrickEndEvent, session: UserSession): JsObject = {
    val gameLobby = session.gameLobby
    Json.obj(
      "playerwon" -> event.winner.name,
      "playersin" -> gameLobby.getLogic.getCurrentMatch.get.playersIn.map(player => player.name),
      "tricklist" -> gameLobby.getLogic.getCurrentRound.get.tricklist.map(trick => trick.winner.map(player => player.name).getOrElse("Trick in Progress"))
    )
  }
}

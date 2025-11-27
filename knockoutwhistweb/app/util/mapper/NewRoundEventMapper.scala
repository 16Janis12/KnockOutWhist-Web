package util.mapper

import de.knockoutwhist.events.global.NewRoundEvent
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object NewRoundEventMapper extends SimpleEventMapper[NewRoundEvent]{
  override def id: String = "NewRoundEvent"

  override def toJson(event: NewRoundEvent, session: UserSession): JsObject = {
    val gameLobby = session.gameLobby
    Json.obj(
      "trumpsuit" -> gameLobby.getLogic.getCurrentRound.get.trumpSuit.toString,
      "players" -> gameLobby.getLogic.getCurrentMatch.get.playersIn.map(player => player.toString)
    )
  }
}

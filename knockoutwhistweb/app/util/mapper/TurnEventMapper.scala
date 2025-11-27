package util.mapper

import de.knockoutwhist.events.global.TurnEvent
import de.knockoutwhist.player.AbstractPlayer
import model.sessions.UserSession
import play.api.libs.json.{JsArray, JsObject, Json}

object TurnEventMapper extends SimpleEventMapper[TurnEvent] {

  override def id: String = "TurnEvent"

  override def toJson(event: TurnEvent, session: UserSession): JsObject = {
    val nextPlayers = if (session.gameLobby.logic.getPlayerQueue.isEmpty) {
      Json.arr()
    } else {
      val queue = session.gameLobby.logic.getPlayerQueue.get
      JsArray(
        queue.duplicate().map(player => mapPlayer(player)).toList
      )
    }
    
    Json.obj(
      "currentPlayer" -> mapPlayer(event.player),
      "nextPlayers" -> nextPlayers
    )
  }

  private def mapPlayer(player: AbstractPlayer): JsObject = {
    Json.obj(
      "name" -> player.name,
      "dog" -> player.isInDogLife
    )
  }

}

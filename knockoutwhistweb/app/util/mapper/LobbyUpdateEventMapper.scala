package util.mapper

import events.LobbyUpdateEvent
import model.sessions.UserSession
import play.api.libs.json.{JsArray, JsObject, Json}

object LobbyUpdateEventMapper extends SimpleEventMapper[LobbyUpdateEvent] {

  override def id: String = "LobbyUpdateEvent"

  override def toJson(event: LobbyUpdateEvent, session: UserSession): JsObject = {
    Json.obj(
      "host" -> session.host,
      "maxPlayers" -> session.gameLobby.maxPlayers,
      "players" -> JsArray(session.gameLobby.getPlayers.values.map(player => {
        Json.obj(
          "id" -> player.id,
          "name" -> player.name,
          "self" -> (player.id == session.user.id)
        )
      }).toList)
    )
  }

}

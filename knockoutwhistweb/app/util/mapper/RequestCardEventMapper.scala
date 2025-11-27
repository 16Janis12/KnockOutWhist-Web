package util.mapper

import de.knockoutwhist.events.player.RequestCardEvent
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object RequestCardEventMapper extends SimpleEventMapper[RequestCardEvent]{
  override def id: String = "RequestCardEvent"

  override def toJson(event: RequestCardEvent, session: UserSession): JsObject = {
    Json.obj(
      "player" -> event.player.name
    )
  }
}

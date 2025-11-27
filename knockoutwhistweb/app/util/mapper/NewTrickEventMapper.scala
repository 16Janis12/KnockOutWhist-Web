package util.mapper

import de.knockoutwhist.events.global.NewTrickEvent
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object NewTrickEventMapper extends SimpleEventMapper[NewTrickEvent]{
  override def id: String = "NewTrickEvent"

  override def toJson(event: NewTrickEvent, session: UserSession): JsObject = {
    Json.obj()
  }
}

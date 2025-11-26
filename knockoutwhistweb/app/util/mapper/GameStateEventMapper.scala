package util.mapper

import controllers.IngameController
import de.knockoutwhist.events.global.GameStateChangeEvent
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object GameStateEventMapper extends SimpleEventMapper[GameStateChangeEvent] {

  override def id: String = "GameStateChangeEvent"

  override def toJson(event: GameStateChangeEvent, session: UserSession): JsObject = {
    Json.obj(
      //Title
      "content" -> IngameController.returnInnerHTML(session.gameLobby, event.newState, session.user).toString
    )
  }
}

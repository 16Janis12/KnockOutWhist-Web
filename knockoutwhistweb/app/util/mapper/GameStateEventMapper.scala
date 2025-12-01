package util.mapper

import controllers.IngameController
import de.knockoutwhist.events.global.GameStateChangeEvent
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}
import util.GameUtil

object GameStateEventMapper extends SimpleEventMapper[GameStateChangeEvent] {

  override def id: String = "GameStateChangeEvent"

  override def toJson(event: GameStateChangeEvent, session: UserSession): JsObject = {
    Json.obj(
      "title" -> ("Knockout Whist - " + GameUtil.stateToTitle(event.newState)),
      "content" -> IngameController.returnInnerHTML(session.gameLobby, event.newState, session.user).toString
    )
  }
}

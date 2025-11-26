package util.mapper

import controllers.routes
import events.{KickEvent, LeftEvent}
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object LeftEventMapper extends SimpleEventMapper[LeftEvent] {

  override def id: String = "LeftEvent"

  override def toJson(event: LeftEvent, session: UserSession): JsObject = {
    Json.obj(
      "url" -> routes.MainMenuController.mainMenu().url,
      "content" -> views.html.mainmenu.creategame(Some(session.user)).toString
    )
  }

}

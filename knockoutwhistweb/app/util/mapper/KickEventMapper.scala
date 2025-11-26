package util.mapper

import controllers.routes
import events.KickEvent
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object KickEventMapper extends SimpleEventMapper[KickEvent] {

  override def id: String = "KickEvent"

  override def toJson(event: KickEvent, session: UserSession): JsObject = {
    Json.obj(
      "url" -> routes.MainMenuController.mainMenu().url,
      "content" -> views.html.mainmenu.creategame(Some(session.user)).toString,
    )
  }

}

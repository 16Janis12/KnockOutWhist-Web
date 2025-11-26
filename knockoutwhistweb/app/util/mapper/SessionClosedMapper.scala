package util.mapper

import controllers.routes
import de.knockoutwhist.events.global.SessionClosed
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object SessionClosedMapper extends SimpleEventMapper[SessionClosed] {

  override def id: String = "SessionClosed"

  override def toJson(event: SessionClosed, session: UserSession): JsObject = {
    Json.obj(
      "url" -> routes.MainMenuController.mainMenu().url,
      "content" -> views.html.mainmenu.creategame(Some(session.user)).toString,
    )
  }

}

package util.mapper

import de.knockoutwhist.utils.events.SimpleEvent
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.JsObject

trait SimpleEventMapper[T <: SimpleEvent] {
  
  def id: String
  def toJson(event: T, session: UserSession): JsObject

}

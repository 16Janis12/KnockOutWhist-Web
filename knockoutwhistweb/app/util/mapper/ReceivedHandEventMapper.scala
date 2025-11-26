package util.mapper

import de.knockoutwhist.events.player.ReceivedHandEvent
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}
import util.WebUIUtils

object ReceivedHandEventMapper extends SimpleEventMapper[ReceivedHandEvent] {

  override def id: String = "ReceivedHandEvent"
  override def toJson(event: ReceivedHandEvent, session: UserSession): JsObject = {
    Json.obj(
      "dog" -> event.player.isInDogLife,
      "hand" -> event.player.currentHand().map(hand => WebUIUtils.handToJson(hand))
    )
  }
}

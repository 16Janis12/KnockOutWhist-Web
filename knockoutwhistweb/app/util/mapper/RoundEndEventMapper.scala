package util.mapper

import controllers.routes
import de.knockoutwhist.events.global.RoundEndEvent
import model.sessions.UserSession
import play.api.libs.json.{JsObject, Json}

object RoundEndEventMapper extends SimpleEventMapper[RoundEndEvent] {

  override def id: String = "RoundEndEvent"

  override def toJson(event: RoundEndEvent, session: UserSession): JsObject = {

    Json.obj(
      "player" -> event.winner.name,
      "tricks" -> event.amountOfTricks
    )
  }

}

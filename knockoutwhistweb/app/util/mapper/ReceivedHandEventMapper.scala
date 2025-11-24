package util.mapper

import de.knockoutwhist.events.player.ReceivedHandEvent
import logic.game.GameLobby
import play.api.libs.json.{JsArray, JsObject, Json}
import util.WebUIUtils

object ReceivedHandEventMapper extends SimpleEventMapper[ReceivedHandEvent] {

  override def id: String = "ReceivedHandEvent"
  override def toJson(event: ReceivedHandEvent, gameLobby: GameLobby): JsObject = {
    Json.obj(
      "dog" -> event.player.isInDogLife,
      "hand" -> event.player.currentHand().map(hand => WebUIUtils.handToJson(hand))
    )
  }
}

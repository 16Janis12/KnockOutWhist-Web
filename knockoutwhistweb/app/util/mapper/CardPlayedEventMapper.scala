package util.mapper

import de.knockoutwhist.events.global.CardPlayedEvent
import model.sessions.UserSession
import play.api.libs.json.{JsArray, JsObject, Json}
import util.WebUIUtils

object CardPlayedEventMapper extends SimpleEventMapper[CardPlayedEvent]{

  override def id: String = "CardPlayedEvent"

  override def toJson(event: CardPlayedEvent, session: UserSession): JsObject = {
    Json.obj(
      "firstCard" -> (if (event.trick.firstCard.isDefined) WebUIUtils.cardtoString(event.trick.firstCard.get) else "BLANK"),
      "playedCards" -> JsArray(event.trick.cards.map { case (card, player) =>
        Json.obj("cardId" -> WebUIUtils.cardtoString(card), "player" -> player.name)
      }.toList)
    )
  }
}

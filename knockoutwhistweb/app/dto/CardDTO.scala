package dto

import de.knockoutwhist.cards.Card
import play.api.libs.json.{Json, OFormat}
import util.WebUIUtils

case class CardDTO(identifier: String, path: String, idx: Int) {
  
  def toCard: Card = {
    WebUIUtils.stringToCard(identifier)
  }
  
}

object CardDTO {
  
  implicit val cardFormat: OFormat[CardDTO] = Json.format[CardDTO]
  
  def apply(card: Card, index: Int = 0): CardDTO = {
    CardDTO(
      identifier = WebUIUtils.cardtoString(card),
      path = WebUIUtils.cardToPath(card),
      idx = index
    )
  }
}

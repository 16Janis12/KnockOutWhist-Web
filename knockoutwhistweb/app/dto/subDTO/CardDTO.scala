package dto.subDTO

import de.knockoutwhist.cards.Card
import util.WebUIUtils

case class CardDTO(identifier: String, path: String, idx: Option[Int]) {
  
  def toCard: Card = {
    WebUIUtils.stringToCard(identifier)
  }
  
}

object CardDTO {
  
  def apply(card: Card, index: Int): CardDTO = {
    CardDTO(
      identifier = WebUIUtils.cardtoString(card),
      path = WebUIUtils.cardToPath(card),
      idx = Some(index)
    )
  }

  def apply(card: Card): CardDTO = {
    CardDTO(
      identifier = WebUIUtils.cardtoString(card),
      path = WebUIUtils.cardToPath(card),
      idx = None
    )
  }
}

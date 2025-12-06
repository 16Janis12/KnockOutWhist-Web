package dto.subDTO

import de.knockoutwhist.cards.Card
import util.WebUIUtils

case class CardDTO(identifier: String, path: String, idx: Int) {
  
  def toCard: Card = {
    WebUIUtils.stringToCard(identifier)
  }
  
}

object CardDTO {
  
  def apply(card: Card, index: Int = 0): CardDTO = {
    CardDTO(
      identifier = WebUIUtils.cardtoString(card),
      path = WebUIUtils.cardToPath(card),
      idx = index
    )
  }
}

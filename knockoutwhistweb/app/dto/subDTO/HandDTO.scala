package dto.subDTO

import de.knockoutwhist.cards.Hand

case class HandDTO(card: List[CardDTO])

object HandDTO {
  
  def apply(hand: Hand): HandDTO = {
    HandDTO(
      card = hand.cards.zipWithIndex.map { case (card, idx) => CardDTO(card, idx) }
    )
  }
  
}

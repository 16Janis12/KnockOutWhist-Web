package dto.subDTO

import de.knockoutwhist.cards.Hand

case class HandDTO(cards: List[CardDTO])

object HandDTO {
  
  def apply(hand: Hand): HandDTO = {
    HandDTO(
      cards = hand.cards.zipWithIndex.map { case (card, idx) => CardDTO(card, idx) }
    )
  }
  
}

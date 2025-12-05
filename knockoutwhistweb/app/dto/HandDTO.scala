package dto

import de.knockoutwhist.cards.Hand
import play.api.libs.json.{Json, OFormat}

case class HandDTO(card: List[CardDTO])

object HandDTO {
  
  implicit val handFormat: OFormat[HandDTO] = Json.format[HandDTO]
  
  def apply(hand: Hand): HandDTO = {
    HandDTO(
      card = hand.cards.zipWithIndex.map { case (card, idx) => CardDTO(card, idx) }
    )
  }
  
}

package dto

import de.knockoutwhist.rounds.Trick
import play.api.libs.json.{Json, OFormat}

case class TrickDTO(cards: Map[PlayerDTO, CardDTO], firstCard: Option[CardDTO], winner: Option[PlayerDTO])

object TrickDTO {

  implicit val trickFormat: OFormat[TrickDTO] = Json.format[TrickDTO]

  def apply(trick: Trick): TrickDTO = {
    TrickDTO(
      cards = trick.cards.map { case (card, player) => PlayerDTO(player) -> CardDTO(card) },
      firstCard = trick.firstCard.map(card => CardDTO(card)),
      winner = trick.winner.map(player => PlayerDTO(player))
    )
  }

}

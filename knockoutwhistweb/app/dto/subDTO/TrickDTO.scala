package dto.subDTO

import de.knockoutwhist.rounds.Trick

case class TrickDTO(cards: Map[String, CardDTO], firstCard: Option[CardDTO], winner: Option[PlayerDTO])

object TrickDTO {

  def apply(trick: Trick): TrickDTO = {
    TrickDTO(
      cards = trick.cards.map { case (card, player) => player.id.toString -> CardDTO(card) },
      firstCard = trick.firstCard.map(card => CardDTO(card)),
      winner = trick.winner.map(player => PlayerDTO(player))
    )
  }

}

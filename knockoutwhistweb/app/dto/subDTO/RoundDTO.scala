package dto.subDTO

import de.knockoutwhist.cards.Card
import de.knockoutwhist.cards.CardValue.Ace

case class RoundDTO(trumpSuit: CardDTO, firstRound: Boolean, tricklist: List[TrickDTO])

object RoundDTO {

  def apply(round: de.knockoutwhist.rounds.Round): RoundDTO = {
    RoundDTO(
      trumpSuit = CardDTO(Card(Ace, round.trumpSuit)),
      firstRound = round.firstRound,
      tricklist = round.tricklist.map(trick => TrickDTO(trick))
    )
  }

}

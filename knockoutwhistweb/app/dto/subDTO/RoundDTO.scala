package dto.subDTO

import de.knockoutwhist.cards.Card
import de.knockoutwhist.cards.CardValue.Ace
import de.knockoutwhist.rounds.{Match, Round}

case class RoundDTO(trumpSuit: CardDTO, playersIn: Seq[PlayerDTO], firstRound: Boolean, trickList: List[TrickDTO])

object RoundDTO {

  def apply(round: Round, matchImpl: Option[Match]): RoundDTO = {
    RoundDTO(
      trumpSuit = CardDTO(Card(Ace, round.trumpSuit)),
      playersIn = matchImpl.map(_.playersIn.map(PlayerDTO(_))).getOrElse(Seq.empty),
      firstRound = round.firstRound,
      trickList = round.tricklist.map(trick => TrickDTO(trick))
    )
  }

}

package dto

import de.knockoutwhist.cards.Card
import de.knockoutwhist.cards.CardValue.Ace
import play.api.libs.json.{Json, OFormat}

case class RoundDTO(trumpSuit: CardDTO, firstRound: Boolean, tricklist: List[TrickDTO], winner: Option[PlayerDTO])

object RoundDTO {

  implicit val roundFormat: OFormat[RoundDTO] = Json.format[RoundDTO]

  def apply(round: de.knockoutwhist.rounds.Round): RoundDTO = {
    RoundDTO(
      trumpSuit = CardDTO(Card(Ace, round.trumpSuit)),
      firstRound = round.firstRound,
      tricklist = round.tricklist.map(trick => TrickDTO(trick)),
      winner = round.winner.map(player => PlayerDTO(player))
    )
  }

}

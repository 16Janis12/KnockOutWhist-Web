package dto.subDTO

import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.rounds.Match

case class PodiumPlayerDTO(
                          player: PlayerDTO,
                          position: Int,
                          roundsWon: Int,
                          tricksWon: Int
                          )

object PodiumPlayerDTO {

  def apply(gameLogic: GameLogic, player: AbstractPlayer): PodiumPlayerDTO = {
    val matchImplOpt = gameLogic.getCurrentMatch
    if (matchImplOpt.isEmpty) {
      throw new IllegalStateException("No current match available in game logic")
    }
    val matchImpl: Match = matchImplOpt.get
    var roundsWon = 0
    var tricksWon = 0
    for (round <- matchImpl.roundlist) {
      if (round.winner.contains(player)) {
        roundsWon += 1
      }
      for (trick <- round.tricklist) {
        if (trick.winner.contains(player)) {
          tricksWon += 1
        }
      }
    }

    PodiumPlayerDTO(
      player = PlayerDTO(player),
      position = if (gameLogic.getWinner.contains(player)) {
        1
      } else {
        2
      },
      roundsWon = roundsWon,
      tricksWon = tricksWon
    )
  }

}
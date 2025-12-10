package dto

import dto.subDTO.PodiumPlayerDTO
import logic.game.GameLobby
import model.users.User

case class WonInfoDTO(
                     gameId: String,
                     winner: Option[PodiumPlayerDTO],
                     allPlayers: Seq[PodiumPlayerDTO]
                     )

object WonInfoDTO {

  def apply(lobby: GameLobby, user: User): WonInfoDTO = {
    val matchImpl = lobby.logic.getCurrentMatch
    if (matchImpl.isEmpty) {
      throw new IllegalStateException("No current match available in game logic")
    }
    val allPlayersDTO: Seq[PodiumPlayerDTO] = matchImpl.get.totalplayers.map { player =>
      PodiumPlayerDTO(lobby.logic, player)
    }

    val selfPlayerDTO = lobby.getPlayerByUser(user)
    val winnerDTO = lobby.logic.getWinner

    WonInfoDTO(
      gameId = lobby.id,
      winner = winnerDTO.map(player => PodiumPlayerDTO(lobby.logic, player)),
      allPlayers = allPlayersDTO
    )
  }

}
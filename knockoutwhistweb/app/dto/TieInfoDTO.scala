package dto

import dto.subDTO.PlayerDTO
import logic.game.GameLobby
import model.users.User

import scala.util.Try

case class TieInfoDTO(gameId: String, currentPlayer: Option[PlayerDTO], self: Option[PlayerDTO], tiedPlayers: Seq[PlayerDTO], highestAmount: Int)

object TieInfoDTO {

  def apply(lobby: GameLobby, user: User): TieInfoDTO = {
    val selfPlayer = Try {
      Some(lobby.getPlayerByUser(user))
    }.getOrElse(None)

    TieInfoDTO(
      gameId = lobby.id,
      currentPlayer = lobby.logic.playerTieLogic.currentTiePlayer().map(PlayerDTO.apply),
      self = selfPlayer.map(PlayerDTO.apply),
      tiedPlayers = lobby.logic.playerTieLogic.getTiedPlayers.map(PlayerDTO.apply),
      highestAmount = lobby.logic.playerTieLogic.highestAllowedNumber()
    )
  }
  
}
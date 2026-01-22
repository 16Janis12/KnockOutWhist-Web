package dto

import de.knockoutwhist.cards.Card
import dto.subDTO.{CardDTO, PlayerDTO}
import logic.game.GameLobby
import model.users.User

import scala.util.Try

case class TieInfoDTO(gameId: String, currentPlayer: Option[PlayerDTO], self: Option[PlayerDTO], tiedPlayers: Seq[PlayerDTO], highestAmount: Int, selectedCards: Map[String, CardDTO], winners: Option[Seq[PlayerDTO]])

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
      highestAmount = lobby.logic.playerTieLogic.highestAllowedNumber(),
      selectedCards = lobby.logic.playerTieLogic.getSelectedCard.map {
        case (player, card) => player.id.toString -> CardDTO(card)
      },
      winners = Some(lobby.logic.playerTieLogic.getSelectedCard.filter((_,card) => card == lobby.logic.playerTieLogic.getSelectedCard.values.maxBy(_.cardValue.ordinal)).keySet.toList.map(PlayerDTO.apply))
    )
  }
  
}
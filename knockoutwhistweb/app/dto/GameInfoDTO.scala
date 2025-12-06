package dto

import dto.subDTO.*
import logic.game.GameLobby
import model.users.User

import scala.util.Try

case class GameInfoDTO(
                      gameId: String,
                      self: Option[PlayerDTO],
                      hand: Option[HandDTO],
                      playerQueue: PlayerQueueDTO,
                      currentTrick: Option[TrickDTO],
                      currentRound: Option[RoundDTO]
                      )

object GameInfoDTO {

  def apply(lobby: GameLobby, user: User): GameInfoDTO = {
    val selfPlayer = Try {
      Some(lobby.getPlayerByUser(user))
    }.getOrElse(None)
    
    GameInfoDTO(
      gameId = lobby.id,
      self = selfPlayer.map(PlayerDTO(_)),
      hand = selfPlayer.flatMap(_.currentHand()).map(HandDTO(_)),
      playerQueue = PlayerQueueDTO(lobby.logic),
      currentTrick = lobby.logic.getCurrentTrick.map(TrickDTO(_)),
      currentRound = lobby.logic.getCurrentRound.map(RoundDTO(_))
    )
  }

}
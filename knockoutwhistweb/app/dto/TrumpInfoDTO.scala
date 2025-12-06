package dto

import dto.subDTO.{HandDTO, PlayerDTO}
import logic.game.GameLobby
import model.users.User

import scala.util.Try

case class TrumpInfoDTO(
                       chooser: Option[PlayerDTO],
                       self: Option[PlayerDTO],
                       selfHand: Option[HandDTO],
                       )

object TrumpInfoDTO {

  def apply(lobby: GameLobby, user: User): TrumpInfoDTO = {
    val selfPlayer = Try {
      Some(lobby.getPlayerByUser(user))
    }.getOrElse(None)

    TrumpInfoDTO(
      chooser = lobby.logic.getTrumpPlayer.map(PlayerDTO(_)),
      self = selfPlayer.map(PlayerDTO(_)),
      selfHand = selfPlayer.flatMap(_.currentHand()).map(HandDTO(_))
    )
  }

}

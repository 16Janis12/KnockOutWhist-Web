package dto

import dto.subDTO.UserDTO
import logic.game.GameLobby
import model.users.User

case class LobbyInfoDTO(gameId: String, users: List[UserDTO], self: UserDTO, maxPlayers: Int)

object LobbyInfoDTO {

  def apply(lobby: GameLobby, user: User): LobbyInfoDTO = {
    val session = lobby.getUserSession(user.id)
    
    LobbyInfoDTO(
      gameId = lobby.id,
      users = lobby.getPlayers.values.map(user => UserDTO(user)).toList,
      self = UserDTO(session),
      maxPlayers = lobby.maxPlayers,
    )
  }

}

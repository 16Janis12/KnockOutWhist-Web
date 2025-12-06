package dto.subDTO

import model.sessions.UserSession

case class UserDTO(id: String, username: String, host: Boolean = false)

object UserDTO {

  def apply(user: UserSession): UserDTO = {
    UserDTO(
      id = user.id.toString,
      username = user.name,
      host = user.host
    )
  }

}
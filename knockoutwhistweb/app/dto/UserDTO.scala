package dto

import model.users.User
import play.api.libs.json.{Json, OFormat}

case class UserDTO(id: String, username: String)

object UserDTO {

  implicit val userFormat: OFormat[UserDTO] = Json.format[UserDTO]

  def apply(user: User): UserDTO = {
    UserDTO(
      id = user.id.toString,
      username = user.name
    )
  }

}
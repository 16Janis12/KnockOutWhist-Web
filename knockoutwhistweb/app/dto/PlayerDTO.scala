package dto

import de.knockoutwhist.player.AbstractPlayer
import play.api.libs.json.{Json, OFormat}

case class PlayerDTO(id: String, name: String, dogLife: Boolean) 

object PlayerDTO {

  implicit val playerFormat: OFormat[PlayerDTO] = Json.format[PlayerDTO]
  
  def apply(player: AbstractPlayer): PlayerDTO = {
    PlayerDTO(
      id = player.id.toString,
      name = player.name,
      dogLife = player.isInDogLife
    )
  }
  
}
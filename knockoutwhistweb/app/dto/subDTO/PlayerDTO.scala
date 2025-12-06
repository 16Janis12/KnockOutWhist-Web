package dto.subDTO

import de.knockoutwhist.player.AbstractPlayer

case class PlayerDTO(id: String, name: String, dogLife: Boolean) 

object PlayerDTO {
  
  def apply(player: AbstractPlayer): PlayerDTO = {
    PlayerDTO(
      id = player.id.toString,
      name = player.name,
      dogLife = player.isInDogLife
    )
  }
  
}
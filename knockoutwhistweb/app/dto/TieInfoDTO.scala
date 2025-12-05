package dto

import de.knockoutwhist.control.sublogic.PlayerTieLogic
import play.api.libs.json.{Json, OFormat}

case class TieInfoDTO(currentPlayer: Option[PlayerDTO], tiedPlayers: Seq[PlayerDTO], highestAmount: Int)

object TieInfoDTO {

  implicit val tieInfoFormat: OFormat[TieInfoDTO] = Json.format[TieInfoDTO]
  
  def apply(tieInput: PlayerTieLogic): Unit = {
    TieInfoDTO(
      currentPlayer = tieInput.currentTiePlayer().map(PlayerDTO.apply),
      tiedPlayers = tieInput.getTiedPlayers.map(PlayerDTO.apply),
      highestAmount = tieInput.highestAllowedNumber()
    )
  }
  
}
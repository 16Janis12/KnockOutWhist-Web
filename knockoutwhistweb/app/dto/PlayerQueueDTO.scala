package dto

import de.knockoutwhist.control.GameLogic
import play.api.libs.json.{Json, OFormat}

case class PlayerQueueDTO(currentPlayer: Option[PlayerDTO], queue: Seq[PlayerDTO])

object PlayerQueueDTO {

  implicit val queueFormat: OFormat[PlayerQueueDTO] = Json.format[PlayerQueueDTO]

  def apply(logic: GameLogic): PlayerQueueDTO = {
    val currentPlayerDTO = logic.getCurrentPlayer.map(PlayerDTO(_))
    val queueDTO = logic.getPlayerQueue.map(_.duplicate().flatMap(player => Some(PlayerDTO(player))).toSeq)
    if (queueDTO.isEmpty) {
      PlayerQueueDTO(currentPlayerDTO, Seq.empty)
    } else {
      PlayerQueueDTO(currentPlayerDTO, queueDTO.get)
    }
  }

}

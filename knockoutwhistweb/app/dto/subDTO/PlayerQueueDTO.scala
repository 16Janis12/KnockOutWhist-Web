package dto.subDTO

import de.knockoutwhist.control.GameLogic

case class PlayerQueueDTO(currentPlayer: Option[PlayerDTO], queue: Seq[PlayerDTO])

object PlayerQueueDTO {

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

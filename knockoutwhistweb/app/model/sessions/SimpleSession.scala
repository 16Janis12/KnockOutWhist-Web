package model.sessions

import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.utils.events.SimpleEvent

import java.util.UUID

case class SimpleSession(id: UUID, player: AbstractPlayer) extends PlayerSession {

  def name: String = player.name

  override def updatePlayer(event: SimpleEvent): Unit = {
  }
}

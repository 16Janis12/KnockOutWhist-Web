package model.sessions

import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.utils.events.SimpleEvent

import java.util.UUID

trait PlayerSession {
  
  def id: UUID
  def name: String
  def player: AbstractPlayer
  def updatePlayer(event: SimpleEvent): Unit
  
}

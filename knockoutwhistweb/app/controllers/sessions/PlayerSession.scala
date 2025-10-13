package controllers.sessions

import de.knockoutwhist.utils.events.SimpleEvent

import java.util.UUID

trait PlayerSession {
  
  def id: UUID
  def updatePlayer(event: SimpleEvent): Unit
  
}

package controllers

import controllers.sessions.PlayerSession
import de.knockoutwhist.utils.events.SimpleEvent

import java.util.UUID
import scala.collection.mutable

object PodGameManager {
  
  private val sessions: mutable.Map[UUID, PlayerSession] = mutable.Map()
  
  def addSession(session: PlayerSession): Unit = {
    sessions.put(session.id, session)
  }
  
  def clearSessions(): Unit = {
    sessions.clear()
  }
  
  def identify(id: UUID): Option[PlayerSession] = {
    sessions.get(id)
  }
  
  def transmit(id: UUID, event: SimpleEvent): Unit = {
    identify(id).foreach(_.updatePlayer(event))
  }
  
  def transmitAll(event: SimpleEvent): Unit = {
    sessions.foreach(session => session._2.updatePlayer(event))
  }
  
  def listSessions(): List[UUID] = {
    sessions.keys.toList
  }

}

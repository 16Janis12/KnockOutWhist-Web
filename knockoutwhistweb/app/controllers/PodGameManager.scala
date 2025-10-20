package controllers

import controllers.sessions.PlayerSession
import de.knockoutwhist.utils.events.SimpleEvent

import java.util.UUID
import scala.collection.mutable

object PodGameManager {
  
  
  private val gamesession: mutable.Map[UUID, Gamesession] = mutable.Map()
  
  def addGame(session: PlayerSession, gamesession: Gamesession): Unit = {
    gamesession.put(session.id, gamesession)
  }
  def createGame(player: String): Unit = {
    val game = Gamesession(UUID.randomUUID())
    
  }
  def clearSessions(): Unit = {
    gamesession.clear()
  }
  
  def identify(id: UUID): Option[PlayerSession] = {
    gamesession.get(id)
  }
  
  def transmit(id: UUID, event: SimpleEvent): Unit = {
    identify(id).foreach(_.updatePlayer(event))
  }
  
  def transmitAll(event: SimpleEvent): Unit = {
    gamesession.foreach(session => session._2.updatePlayer(event))
  }
  
  def listSessions(): List[UUID] = {
    sessions.keys.toList
  }

}

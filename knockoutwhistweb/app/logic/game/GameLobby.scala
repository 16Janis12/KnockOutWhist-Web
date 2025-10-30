package logic.game

import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.events.player.PlayerEvent
import de.knockoutwhist.player.Playertype.HUMAN
import de.knockoutwhist.player.{AbstractPlayer, PlayerFactory}
import de.knockoutwhist.rounds.Match
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}
import exceptions.{NotHostException, NotInThisGameException, NotInteractableException}
import model.sessions.UserSession
import model.users.User

import java.util.UUID
import scala.collection.mutable.ListBuffer

class GameLobby(val logic: GameLogic, val id: String, internalId: UUID) extends EventListener{
  logic.addListener(this)
  logic.createSession()

  val users: Map[UUID, UserSession] = Map()

  override def listen(event: SimpleEvent): Unit = {
    event match {
      case event: PlayerEvent =>
        users.get(event.playerId).foreach(session => session.updatePlayer(event))
      case event: SimpleEvent =>
        users.values.foreach(session => session.updatePlayer(event))
    }
  }

  def startGame(user: User): Unit = {
    val sessionOpt = users.get(user.id)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    if (!sessionOpt.get.host) {
      throw new NotHostException("Only the host can start the game!")
    }
    val playerNamesList = ListBuffer[AbstractPlayer]()
    users.values.foreach { player =>
      playerNamesList += PlayerFactory.createPlayer(player.name, player.id, HUMAN)
    }
    logic.createMatch(playerNamesList.toList)
    logic.controlMatch()
  }

  def playCard(user: User, card: Int): Unit = {
    val sessionOpt = users.get(user.id)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    if (!sessionOpt.get.canInteract) {
      throw new NotInteractableException("You can't play a card!")
    }
    
  }

  
  //-------------------
  
  private def getMatch: Match = {
    val matchOpt = logic.getCurrentMatch
    if (matchOpt.isEmpty) {
      throw new IllegalStateException("No match is currently running!")
    }
    matchOpt.get
  }
  
}

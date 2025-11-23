package logic

import com.google.inject.{Guice, Injector}
import de.knockoutwhist.components.Configuration
import de.knockoutwhist.control.controllerBaseImpl.BaseGameLogic
import di.KnockOutWebConfigurationModule
import logic.game.GameLobby
import model.users.User
import util.GameUtil

import javax.inject.Singleton
import scala.collection.mutable

object PodManager {

  val TTL: Long = System.currentTimeMillis() + 86400000L // 24 hours in milliseconds
  val podIp: String = System.getenv("POD_IP")
  val podName: String = System.getenv("POD_NAME")

  private val sessions: mutable.Map[String, GameLobby] = mutable.Map()
  private val userSession: mutable.Map[User, String] = mutable.Map()
  private val injector: Injector = Guice.createInjector(KnockOutWebConfigurationModule())

  def createGame(
                  host: User,
                  name: String,
                  maxPlayers: Int
                ): GameLobby = {
    val gameLobby = GameLobby(
      logic = BaseGameLogic(injector.getInstance(classOf[Configuration])),
      id = GameUtil.generateCode(),
      internalId = java.util.UUID.randomUUID(),
      name = name,
      maxPlayers = maxPlayers,
      host = host
    )
    sessions += (gameLobby.id -> gameLobby)
    userSession += (host -> gameLobby.id)
    gameLobby
  }

  def getGame(gameId: String): Option[GameLobby] = {
    sessions.get(gameId)
  }

  def registerUserToGame(user: User, gameId: String): Boolean = {
    if (sessions.contains(gameId)) {
      userSession += (user -> gameId)
      true
    } else {
      false
    }
  }

  def unregisterUserFromGame(user: User): Unit = {
    userSession.remove(user)
  }

  def identifyGameOfUser(user: User): Option[GameLobby] = {
    userSession.get(user) match {
      case Some(gameId) => sessions.get(gameId)
      case None => None
    }
  }

  private[logic] def removeGame(gameId: String): Unit = {
    sessions.remove(gameId)
    // Also remove all user sessions associated with this game
    userSession.filterInPlace((_, v) => v != gameId)
  }


}

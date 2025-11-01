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

@Singleton
class PodManager {

  val TTL: Long = System.currentTimeMillis() + 86400000L // 24 hours in milliseconds
  val podIp: String = System.getenv("POD_IP")
  val podName: String = System.getenv("POD_NAME")
  
  private val sessions: mutable.Map[String, GameLobby] = mutable.Map()
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
    gameLobby
  }

  def getGame(gameId: String): Option[GameLobby] = {
    sessions.get(gameId)
  }
  
  private[logic] def removeGame(gameId: String): Unit = {
    sessions.remove(gameId)
  }

}

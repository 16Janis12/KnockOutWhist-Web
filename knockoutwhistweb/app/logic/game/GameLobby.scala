package logic.game

import de.knockoutwhist.cards.{Hand, Suit}
import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.control.GameState.{Lobby, MainMenu}
import de.knockoutwhist.control.controllerBaseImpl.sublogic.util.{MatchUtil, PlayerUtil}
import de.knockoutwhist.events.global.{GameStateChangeEvent, SessionClosed}
import de.knockoutwhist.events.player.PlayerEvent
import de.knockoutwhist.player.Playertype.HUMAN
import de.knockoutwhist.player.{AbstractPlayer, PlayerFactory}
import de.knockoutwhist.rounds.{Match, Round, Trick}
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}
import exceptions.*
import logic.PodManager
import model.sessions.{InteractionType, UserSession}
import model.users.User
import play.api.libs.json.{JsObject, Json}

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class GameLobby private(
                         val logic: GameLogic,
                         val id: String,
                         val internalId: UUID,
                         val name: String,
                         val maxPlayers: Int
                       ) extends EventListener {


  private val users: mutable.Map[UUID, UserSession] = mutable.Map()
  logic.addListener(this)
  logic.createSession()

  def addUser(user: User): UserSession = {
    if (users.size >= maxPlayers) throw new GameFullException("The game is full!")
    if (users.contains(user.id)) throw new IllegalArgumentException("User is already in the game!")
    if (logic.getCurrentState != Lobby) throw new IllegalStateException("The game has already started!")
    val userSession = new UserSession(
      user = user,
      host = false,
      gameLobby = this
    )
    users += (user.id -> userSession)
    PodManager.registerUserToGame(user, id)
    //TODO : transmit Lobby Update transmitToAll()
    userSession
  }

  override def listen(event: SimpleEvent): Unit = {
    event match {
      case event: PlayerEvent =>
        users.get(event.playerId).foreach(session => session.updatePlayer(event))
      case event: GameStateChangeEvent =>
        if (event.oldState == MainMenu && event.newState == Lobby) {
          return
        }
        users.values.foreach(session => session.updatePlayer(event))
      case event: SimpleEvent =>
        users.values.foreach(session => session.updatePlayer(event))
    }
  }

  /**
   * Start the game if the user is the host.
   *
   * @param user the user who wants to start the game.
   */
  def startGame(user: User): Unit = {
    val sessionOpt = users.get(user.id)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    if (!sessionOpt.get.host) {
      throw new NotHostException("Only the host can start the game!")
    }
    if (logic.getCurrentState != Lobby) {
      throw new IllegalStateException("The game has already started!")
    }
    val playerNamesList = ListBuffer[AbstractPlayer]()
    users.values.foreach { player =>
      playerNamesList += PlayerFactory.createPlayer(player.name, player.id, HUMAN)
    }
    if (playerNamesList.size < 2) {
      throw new NotEnoughPlayersException("Not enough players to start the game!")
    }
    logic.createMatch(playerNamesList.toList)
    logic.controlMatch()
  }

  /**
   * Remove the user from the game lobby.
   *
   * @param user the user who wants to leave the game.
   */
  def leaveGame(userId: UUID): Unit = {
    val sessionOpt = users.get(userId)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    if (sessionOpt.get.host) {
      logic.invoke(SessionClosed())
      users.clear()
      PodManager.removeGame(id)
      return
    }
    sessionOpt.get.websocketActor.foreach(act => act.transmitJsonToClient(Json.obj(
      "id" -> "-1",
      "event" -> "SessionClosed",
      "data" -> Json.obj(
        "reason" -> "You left the game (or got kicked)."
      )
    )))
    users.remove(userId)
    PodManager.unregisterUserFromGame(sessionOpt.get.user)
    //TODO: transmit Lobby Update transmitToAll()
  }

  /**
   * Play a card from the player's hand.
   *
   * @param userSession the user session of the player.
   * @param cardIndex   the index of the card in the player's hand.
   */
  def playCard(userSession: UserSession, cardIndex: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.Card)
    if (player.isInDogLife) {
      throw new CantPlayCardException("You are in dog life!")
    }
    val hand = getHand(player)
    val card = hand.cards(cardIndex)
    if (!PlayerUtil.canPlayCard(card, getRound, getTrick, player)) {
      throw new CantPlayCardException("You can't play this card!")
    }
    userSession.resetCanInteract()
    logic.playerInputLogic.receivedCard(card)
  }

  private def getHand(player: AbstractPlayer): Hand = {
    val handOption = player.currentHand()
    if (handOption.isEmpty) {
      throw new IllegalStateException("You have no cards!")
    }
    handOption.get
  }

  private def getRound: Round = {
    val roundOpt = logic.getCurrentRound
    if (roundOpt.isEmpty) {
      throw new IllegalStateException("No round is currently running!")
    }
    roundOpt.get
  }

  private def getTrick: Trick = {
    val trickOpt = logic.getCurrentTrick
    if (trickOpt.isEmpty) {
      throw new IllegalStateException("No trick is currently running!")
    }
    trickOpt.get
  }

  /**
   * Play a card from the player's hand while in dog life or skip the round.
   *
   * @param userSession the user session of the player.
   * @param cardIndex   the index of the card in the player's hand or -1 if the player wants to skip the round.
   */
  def playDogCard(userSession: UserSession, cardIndex: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.DogCard)
    if (!player.isInDogLife) {
      throw new CantPlayCardException("You are not in dog life!")
    }
    if (cardIndex == -1) {
      if (MatchUtil.dogNeedsToPlay(getMatch, getRound)) {
        throw new CantPlayCardException("You can't skip this round!")
      }
      logic.playerInputLogic.receivedDog(None)
      return
    }
    val hand = getHand(player)
    val card = hand.cards(cardIndex)
    userSession.resetCanInteract()
    logic.playerInputLogic.receivedDog(Some(card))
  }

  /**
   * Select the trump suit for the round.
   *
   * @param userSession the user session of the player.
   * @param trumpIndex  the index of the trump suit.
   */
  def selectTrump(userSession: UserSession, trumpIndex: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.TrumpSuit)
    val trumpSuits = Suit.values.toList
    val selectedTrump = trumpSuits(trumpIndex)
    userSession.resetCanInteract()
    logic.playerInputLogic.receivedTrumpSuit(selectedTrump)
  }


  //-------------------

  private def getPlayerInteractable(userSession: UserSession, iType: InteractionType): AbstractPlayer = {
    if (!userSession.lock.isHeldByCurrentThread) {
      throw new IllegalStateException("The user session is not locked!")
    }
    if (userSession.canInteract.isEmpty || userSession.canInteract.get != iType) {
      throw new NotInteractableException("You can't play a card!")
    }
    getPlayerBySession(userSession)
  }

  /**
   *
   * @param userSession
   * @param tieNumber
   */
  def selectTie(userSession: UserSession, tieNumber: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.TieChoice)
    userSession.resetCanInteract()
    logic.playerTieLogic.receivedTieBreakerCard(tieNumber)
  }

  def returnToLobby(userSession: UserSession): Unit = {
    if (!users.contains(userSession.id)) {
      throw new NotInThisGameException("You are not in this game!")
    }
    val session = users(userSession.id)
    if (session != userSession) {
      throw new IllegalArgumentException("User session does not match!")
    }
    if (!session.host)
      throw new NotHostException("Only the host can return to the lobby!")
    logic.createSession()
  }

  def getPlayerByUser(user: User): AbstractPlayer = {
    getPlayerBySession(getUserSession(user.id))
  }

  def getUserSession(userId: UUID): UserSession = {
    val sessionOpt = users.get(userId)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    sessionOpt.get
  }

  private def getPlayerBySession(userSession: UserSession): AbstractPlayer = {
    val playerOption = getMatch.totalplayers.find(_.id == userSession.id)
    if (playerOption.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    playerOption.get
  }

  private def getMatch: Match = {
    val matchOpt = logic.getCurrentMatch
    if (matchOpt.isEmpty) {
      throw new IllegalStateException("No match is currently running!")
    }
    matchOpt.get
  }

  def getPlayers: mutable.Map[UUID, UserSession] = {
    users.clone()
  }

  def getLogic: GameLogic = {
    logic
  }

  def getUsers: Set[User] = {
    users.values.map(d => d.user).toSet
  }

  private def transmitToAll(event: JsObject): Unit = {
    users.values.foreach(session => {
      session.websocketActor.foreach(act => act.transmitJsonToClient(event))
    })
  }

}

object GameLobby {
  def apply(
             logic: GameLogic,
             id: String,
             internalId: UUID,
             name: String,
             maxPlayers: Int,
             host: User
           ): GameLobby = {
    val lobby = new GameLobby(
      logic = logic,
      id = id,
      internalId = internalId,
      name = name,
      maxPlayers = maxPlayers
    )
    lobby.users += (host.id -> new UserSession(
      user = host,
      host = true,
      gameLobby = lobby
    ))
    lobby
  }
}

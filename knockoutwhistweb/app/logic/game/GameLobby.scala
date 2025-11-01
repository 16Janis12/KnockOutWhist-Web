package logic.game

import de.knockoutwhist.cards.{Hand, Suit}
import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.control.GameState.Lobby
import de.knockoutwhist.control.controllerBaseImpl.sublogic.util.{MatchUtil, PlayerUtil, RoundUtil}
import de.knockoutwhist.events.global.SessionClosed
import de.knockoutwhist.events.player.PlayerEvent
import de.knockoutwhist.player.Playertype.HUMAN
import de.knockoutwhist.player.{AbstractPlayer, PlayerFactory}
import de.knockoutwhist.rounds.{Match, Round, Trick}
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}
import exceptions.{CantPlayCardException, GameFullException, NotHostException, NotInThisGameException, NotInteractableException}
import model.sessions.{InteractionType, UserSession}
import model.users.User

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
  logic.addListener(this)
  logic.createSession()

  private val users: mutable.Map[UUID, UserSession] = mutable.Map()
  
  def addUser(user: User): UserSession = {
    if (users.size >= maxPlayers) throw new GameFullException("The game is full!")
    if (users.contains(user.id)) throw new IllegalArgumentException("User is already in the game!")
    if (logic.getCurrentState != Lobby) throw new IllegalStateException("The game has already started!")
    val userSession = new UserSession(
      user = user,
      host = false
    )
    users += (user.id -> userSession)
    userSession
  }

  override def listen(event: SimpleEvent): Unit = {
    event match {
      case event: PlayerEvent =>
        users.get(event.playerId).foreach(session => session.updatePlayer(event))
      case event: SessionClosed =>
        users.values.foreach(session => session.updatePlayer(event))

      case event: SimpleEvent =>
        users.values.foreach(session => session.updatePlayer(event))
    }
  }

  /**
   * Start the game if the user is the host.
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
    val playerNamesList = ListBuffer[AbstractPlayer]()
    users.values.foreach { player =>
      playerNamesList += PlayerFactory.createPlayer(player.name, player.id, HUMAN)
    }
    logic.createMatch(playerNamesList.toList)
    logic.controlMatch()
  }

  /**
   * Remove the user from the game lobby.
   * @param user the user who wants to leave the game.
   */
  def leaveGame(user: User): Unit = {
    val sessionOpt = users.get(user.id)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    users.remove(user.id)
  }

  /**
   * Play a card from the player's hand.
   * @param userSession the user session of the player.
   * @param cardIndex the index of the card in the player's hand.
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
    logic.playerInputLogic.receivedCard(card)
  }

  /**
   * Play a card from the player's hand while in dog life or skip the round.
   * @param userSession the user session of the player.
   * @param cardIndex the index of the card in the player's hand or -1 if the player wants to skip the round.
   */
  def playDogCard(userSession: UserSession, cardIndex: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.DogCard)
    if (!player.isInDogLife) {
      throw new CantPlayCardException("You are not in dog life!")
    }
    if (cardIndex == -1) {
      if (!MatchUtil.dogNeedsToPlay(getMatch, getRound)) {
        throw new CantPlayCardException("You can't skip this round!")
      }
      logic.playerInputLogic.receivedDog(None)
    }
    val hand = getHand(player)
    val card = hand.cards(cardIndex)
    logic.playerInputLogic.receivedDog(Some(card))
  }

  /**
   * Select the trump suit for the round.
   * @param userSession the user session of the player.
   * @param trumpIndex the index of the trump suit.
   */
  def selectTrump(userSession: UserSession, trumpIndex: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.TrumpSuit)
    val trumpSuits = Suit.values.toList
    val selectedTrump = trumpSuits(trumpIndex)
    logic.playerInputLogic.receivedTrumpSuit(selectedTrump)
  }

  /**
   * 
   * @param userSession
   * @param tieNumber
   */
  def selectTie(userSession: UserSession, tieNumber: Int): Unit = {
    val player = getPlayerInteractable(userSession, InteractionType.TieChoice)
    logic.playerTieLogic.receivedTieBreakerCard(tieNumber)
  }

  
  //-------------------
  
  private def getUserSession(userId: UUID): UserSession = {
    val sessionOpt = users.get(userId)
    if (sessionOpt.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    sessionOpt.get
  }
  
  private def getPlayerInteractable(userSession: UserSession, iType: InteractionType): AbstractPlayer = {
    if (!Thread.holdsLock(userSession.lock)) {
      throw new IllegalStateException("The user session is not locked!")
    }
    if (userSession.canInteract.isEmpty || userSession.canInteract.get != iType) {
      throw new NotInteractableException("You can't play a card!")
    }
    val playerOption = getMatch.totalplayers.find(_.id == userSession.id)
    if (playerOption.isEmpty) {
      throw new NotInThisGameException("You are not in this game!")
    }
    playerOption.get
  }
  
  private def getHand(player: AbstractPlayer): Hand = {
    val handOption = player.currentHand()
    if (handOption.isEmpty) {
      throw new IllegalStateException("You have no cards!")
    }
    handOption.get
  }
  
  private def getMatch: Match = {
    val matchOpt = logic.getCurrentMatch
    if (matchOpt.isEmpty) {
      throw new IllegalStateException("No match is currently running!")
    }
    matchOpt.get
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
      host = true
    ))
    lobby
  }
}

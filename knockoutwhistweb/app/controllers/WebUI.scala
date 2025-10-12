package controllers

import de.knockoutwhist.events.directional.RequestPickTrumpsuitEvent
import de.knockoutwhist.events.round.ShowCurrentTrickEvent
import de.knockoutwhist.ui.UI
import de.knockoutwhist.ui.tui.TUIMain.{init, runLater, start}
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}
import de.knockoutwhist.KnockOutWhist
import de.knockoutwhist.cards.{Card, CardValue, Hand, Suit}
import de.knockoutwhist.control.controllerBaseImpl.{PlayerLogic, TrickLogic}
import de.knockoutwhist.control.{ControlHandler, ControlThread}
import de.knockoutwhist.events.*
import de.knockoutwhist.events.ERROR_STATUS.*
import de.knockoutwhist.events.GLOBAL_STATUS.*
import de.knockoutwhist.events.PLAYER_STATUS.*
import de.knockoutwhist.events.ROUND_STATUS.{PLAYERS_OUT, SHOW_START_ROUND, WON_ROUND}
import de.knockoutwhist.events.cards.{RenderHandEvent, ShowTieCardsEvent}
import de.knockoutwhist.events.directional.*
import de.knockoutwhist.events.round.ShowCurrentTrickEvent
import de.knockoutwhist.events.ui.GameState.MAIN_MENU
import de.knockoutwhist.events.ui.{GameState, GameStateUpdateEvent}
import de.knockoutwhist.events.util.DelayEvent
import de.knockoutwhist.player.Playertype.HUMAN
import de.knockoutwhist.player.{AbstractPlayer, PlayerFactory}
import de.knockoutwhist.ui.UI
import de.knockoutwhist.undo.{UndoManager, UndoneException}
import de.knockoutwhist.utils.CustomThread
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

import java.io.{BufferedReader, InputStreamReader}
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
 
object WebUI extends CustomThread with EventListener with UI {

  override def initial: Boolean = {
    if (init) {
      return false
    }
    init = true
    start()
    true
  }
  setName("WebUI")

  override def instance: CustomThread = WebUI

  var init = false
  override def runLater[R](op: => R): Unit = {
    interrupted.set(true)
    super.runLater(op)
  }
  var latestOutput: String = ""
  private var internState: GameState = GameState.NO_SET
  override def listen(event: SimpleEvent): Unit = {
    runLater {
      event match {
        case event: RenderHandEvent =>
          renderhandmethod(event)
        case event: ShowTieCardsEvent =>
          showtiecardseventmethod(event)
        case event: ShowGlobalStatus =>
          showglobalstatusmethod(event)
        case event: ShowPlayerStatus =>
          showplayerstatusmethod(event)
        case event: ShowRoundStatus =>
          showroundstatusmethod(event)
        case event: ShowErrorStatus =>
          showerrstatmet(event)
        case event: RequestTieNumberEvent =>
          reqnumbereventmet(event)
        case event: RequestCardEvent =>
          reqcardeventmet(event)
        case event: RequestDogPlayCardEvent =>
          reqdogeventmet(event)
        case event: RequestPickTrumpsuitEvent =>
          reqpicktevmet(event)
        case event: ShowCurrentTrickEvent =>
          showcurtrevmet(event)
        case event: GameStateUpdateEvent =>
          if (internState != event.gameState) {
            internState = event.gameState
            if (event.gameState == GameState.MAIN_MENU) {
              mainMenu()
            } else if (event.gameState == GameState.PLAYERS) {
              reqplayersevent()
            }
            Some(true)
          }
        case _ => None
      }
    }
  }


  object WebUICards {
    def renderCardAsString(card: Card): Vector[String] = {
      val lines = "│         │"
      if (card.cardValue == CardValue.Ten) {
        latestOutput += Vector(
          s"┌─────────┐",
          s"│${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}       │",
          lines,
          s"│    ${cardColour(card.suit)}${Console.BOLD}${card.suit.cardType()}${Console.RESET}    │",
          lines,
          s"│       ${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}│",
          s"└─────────┘"
        )
        return Vector(
          s"┌─────────┐",
          s"│${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}       │",
          lines,
          s"│    ${cardColour(card.suit)}${Console.BOLD}${card.suit.cardType()}${Console.RESET}    │",
          lines,
          s"│       ${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}│",
          s"└─────────┘"
        )
      }
      latestOutput += Vector(
        s"┌─────────┐",
        s"│${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}        │",
        lines,
        s"│    ${cardColour(card.suit)}${Console.BOLD}${card.suit.cardType()}${Console.RESET}    │",
        lines,
        s"│        ${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}│",
        s"└─────────┘"
      )
      return Vector(
        s"┌─────────┐",
        s"│${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}        │",
        lines,
        s"│    ${cardColour(card.suit)}${Console.BOLD}${card.suit.cardType()}${Console.RESET}    │",
        lines,
        s"│        ${cardColour(card.suit)}${Console.BOLD}${card.cardValue.cardType()}${Console.RESET}│",
        s"└─────────┘"
      )
    }

    private def cardColour(suit: Suit): String = suit match {
      case Suit.Hearts | Suit.Diamonds => Console.RED
      case Suit.Clubs | Suit.Spades => Console.BLACK
    }

    def renderHandEvent(hand: Hand, showNumbers: Boolean): Vector[String] = {
      val cardStrings = hand.cards.map(WebUICards.renderCardAsString)
      var zipped = cardStrings.transpose
      if (showNumbers) zipped = {
        List.tabulate(hand.cards.length) { i =>
          s"     ${i + 1}     "
        }
      } :: zipped
      latestOutput += zipped.map(_.mkString(" ")).toVector
      zipped.map(_.mkString(" ")).toVector
    }
  }

  //override def initial: Boolean = {
  //if (init) {
  //return false
  //}
  //init = true
  //start()
  //true
  //}

  @tailrec
  private def mainMenu(): Unit = {
    latestOutput = ""
    latestOutput += "Welcome to Knockout Whist\n"
    latestOutput += "Please select an option:\n"
    latestOutput += "1. Start a new match\n"
    latestOutput += "2. Exit\n"
    Try {
      input().toInt
    } match {
      case Success(value) =>
        value match {
          case 1 =>
            ControlThread.runLater {
              KnockOutWhist.config.maincomponent.startMatch()
            }
          case 2 =>
            println("Exiting the game.")
            System.exit(0)
          case _ =>
            showerrstatmet(ShowErrorStatus(INVALID_NUMBER))
            ControlThread.runLater {
              ControlHandler.invoke(DelayEvent(500))
              ControlHandler.invoke(GameStateUpdateEvent(MAIN_MENU))
            }
            mainMenu()
        }
      case Failure(exception) =>
        exception match {
          case undo: UndoneException =>
          case _ =>
            showerrstatmet(ShowErrorStatus(NOT_A_NUMBER))
            ControlThread.runLater {
              ControlHandler.invoke(DelayEvent(500))
              ControlHandler.invoke(GameStateUpdateEvent(MAIN_MENU))
            }
        }
    }
  }

  private def renderhandmethod(event: RenderHandEvent): Option[Boolean] = {
    WebUICards.renderHandEvent(event.hand, event.showNumbers).foreach(println)
    Some(true)
  }

  private def showtiecardseventmethod(event: ShowTieCardsEvent): Option[Boolean] = {
    val a: Array[String] = Array("", "", "", "", "", "", "", "")
    for ((player, card) <- event.card) {
      val playerNameLength = player.name.length
      a(0) += " " + player.name + ":" + (" " * (playerNameLength - 1))
      val rendered = WebUICards.renderCardAsString(card)
      a(1) += " " + rendered(0)
      a(2) += " " + rendered(1)
      a(3) += " " + rendered(2)
      a(4) += " " + rendered(3)
      a(5) += " " + rendered(4)
      a(6) += " " + rendered(5)
      a(7) += " " + rendered(6)
    }
    a.foreach(println)
    Some(true)
  }

  private def showglobalstatusmethod(event: ShowGlobalStatus): Option[Boolean] = {
    event.status match {
      case SHOW_TIE =>
        println("It's a tie! Let's cut to determine the winner.")
        Some(true)
      case SHOW_TIE_WINNER =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[AbstractPlayer]) {
          None
        } else {
          println(s"${event.objects.head.asInstanceOf[AbstractPlayer].name} wins the cut!")
          Some(true)
        }
      case SHOW_TIE_TIE =>
        println("It's a tie again! Let's cut again.")
        Some(true)
      case SHOW_START_MATCH =>
        latestOutput = ""
        println("Starting a new match...")
        wait(1000)
        latestOutput = ""
        Some(true)
      case SHOW_TYPE_PLAYERS =>
        latestOutput += "Please enter the names of the players, separated by a comma."
        Some(true)
      case SHOW_FINISHED_MATCH =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[AbstractPlayer]) {
          None
        } else {
          latestOutput = ""
          latestOutput += s"The match is over. The winner is ${event.objects.head.asInstanceOf[AbstractPlayer]}"
          Some(true)
        }
    }
  }

  private def showplayerstatusmethod(event: ShowPlayerStatus): Option[Boolean] = {
    val player = event.player
    event.status match {
      case SHOW_TURN =>
        println("It's your turn, " + player.name + ".")
        Some(true)
      case SHOW_PLAY_CARD =>
        println("Which card do you want to play?")
        Some(true)
      case SHOW_DOG_PLAY_CARD =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[Boolean]) {
          None
        } else {
          println("You are using your dog life. Do you want to play your final card now?")
          if (event.objects.head.asInstanceOf[Boolean]) {
            println("You have to play your final card this round!")
            println("Please enter y to play your final card.")
            Some(true)
          } else {
            println("Please enter y/n to play your final card.")
            Some(true)
          }
        }
      case SHOW_TIE_NUMBERS =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[Int]) {
          None
        } else {
          println(s"${player.name} enter a number between 1 and ${event.objects.head.asInstanceOf[Int]}.")
          Some(true)
        }
      case SHOW_TRUMPSUIT_OPTIONS =>
        println("Which suit do you want to pick as the next trump suit?")
        println("1: Hearts")
        println("2: Diamonds")
        println("3: Clubs")
        println("4: Spades")
        println()
        Some(true)
      case SHOW_NOT_PLAYED =>
        println(s"Player ${event.player} decided to not play his card")
        Some(true)
      case SHOW_WON_PLAYER_TRICK =>
        println(s"${event.player.name} won the trick.")
        wait(2000)
        latestOutput = ""
        Some(true)
    }
  }

  private def showroundstatusmethod(event: ShowRoundStatus): Option[Boolean] = {
    event.status match {
      case SHOW_START_ROUND =>
        latestOutput = ""
        println(s"Starting a new round. The trump suit is ${event.currentRound.trumpSuit}.")
        wait(2000)
        latestOutput = ""
        Some(true)
      case WON_ROUND =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[AbstractPlayer]) {
          None
        } else {
          println(s"${event.objects.head.asInstanceOf[AbstractPlayer].name} won the round.")
          Some(true)
        }
      case PLAYERS_OUT =>
        println("The following players are out of the game:")
        event.currentRound.playersout.foreach(p => {
          println(p.name)
        })
        Some(true)
    }
  }

  private def showerrstatmet(event: ShowErrorStatus): Option[Boolean] = {
    event.status match {
      case INVALID_NUMBER =>
        println("Please enter a valid number.")
        Some(true)
      case NOT_A_NUMBER =>
        println("Please enter a number.")
        Some(true)
      case INVALID_INPUT =>
        latestOutput += "Please enter a valid input"
        Some(true)
      case INVALID_NUMBER_OF_PLAYERS =>
        latestOutput += "Please enter at least two names."
        Some(true)
      case IDENTICAL_NAMES =>
        latestOutput += "Please enter unique names."
        Some(true)
      case INVALID_NAME_FORMAT =>
        latestOutput += "Please enter valid names. Those can not be empty, shorter than 2 or longer then 10 characters."
        Some(true)
      case WRONG_CARD =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[Card]) {
          None
        } else {
          latestOutput += f"You have to play a card of suit: ${event.objects.head.asInstanceOf[Card].suit}\n"
          Some(true)
        }
    }
  }

  private def reqnumbereventmet(event: RequestTieNumberEvent): Option[Boolean] = {
    val tryTie = Try {
      val number = input().toInt
      if (number < 1 || number > event.remaining) {
        throw new IllegalArgumentException(s"Number must be between 1 and ${event.remaining}")
      }
      number
    }
    if (tryTie.isFailure && tryTie.failed.get.isInstanceOf[UndoneException]) {
      return Some(true)
    }
    ControlThread.runLater {
      KnockOutWhist.config.playerlogcomponent.selectedTie(event.winner, event.matchImpl, event.round, event.playersout, event.cut, tryTie, event.currentStep, event.remaining, event.currentIndex)
    }
    Some(true)
  }

  private def reqcardeventmet(event: RequestCardEvent): Option[Boolean] = {
    val tryCard = Try {
      val card = input().toInt - 1
      if (card < 0 || card >= event.hand.cards.length) {
        throw new IllegalArgumentException(s"Number has to be between 1 and ${event.hand.cards.length}")
      } else {
        event.hand.cards(card)
      }
    }
    if (tryCard.isFailure && tryCard.failed.get.isInstanceOf[UndoneException]) {
      return Some(true)
    }
    ControlThread.runLater {
      KnockOutWhist.config.trickcomponent.controlSuitplayed(tryCard, event.matchImpl, event.round, event.trick, event.currentIndex, event.player)
    }
    Some(true)
  }

  private def reqdogeventmet(event: RequestDogPlayCardEvent): Option[Boolean] = {
    val tryDogCard = Try {
      val card = input()
      if (card.equalsIgnoreCase("y")) {
        Some(event.hand.cards.head)
      } else if (card.equalsIgnoreCase("n") && !event.needstoplay) {
        None
      } else {
        throw new IllegalArgumentException("Didn't want to play card but had to")
      }
    }
    if (tryDogCard.isFailure && tryDogCard.failed.get.isInstanceOf[UndoneException]) {
      return Some(true)
    }
    ControlThread.runLater {
      KnockOutWhist.config.trickcomponent.controlDogPlayed(tryDogCard, event.matchImpl, event.round, event.trick, event.currentIndex, event.player)
    }
    Some(true)
  }

  private def reqplayersevent(): Option[Boolean] = {
    showglobalstatusmethod(ShowGlobalStatus(SHOW_TYPE_PLAYERS))
    val names = Try {
      input().split(",")
    }
    if (names.isFailure && names.failed.get.isInstanceOf[UndoneException]) {
      return Some(true)
    }
    if (names.get.length < 2) {
      showerrstatmet(ShowErrorStatus(INVALID_NUMBER_OF_PLAYERS))
      return reqplayersevent()
    }
    if (names.get.distinct.length != names.get.length) {
      showerrstatmet(ShowErrorStatus(IDENTICAL_NAMES))
      return reqplayersevent()
    }
    if (names.get.count(_.trim.isBlank) > 0
      || names.get.count(_.trim.length <= 2) > 0
      || names.get.count(_.trim.length > 10) > 0) {
      showerrstatmet(ShowErrorStatus(INVALID_NAME_FORMAT))
      return reqplayersevent()
    }
    ControlThread.runLater {
      KnockOutWhist.config
        .maincomponent
        .enteredPlayers(names.get
          .map(s => PlayerFactory.createPlayer(s, playertype = HUMAN))
          .toList)
    }
    Some(true)
  }

  private def reqpicktevmet(event: RequestPickTrumpsuitEvent): Option[Boolean] = {
    val trySuit = Try {
      val suit = input().toInt
      suit match {
        case 1 => Suit.Hearts
        case 2 => Suit.Diamonds
        case 3 => Suit.Clubs
        case 4 => Suit.Spades
        case _ => throw IllegalArgumentException("Didn't enter a number between 1 and 4")
      }
    }
    if (trySuit.isFailure && trySuit.failed.get.isInstanceOf[UndoneException]) {
      return Some(true)
    }
    ControlThread.runLater {
      KnockOutWhist.config.playerlogcomponent.trumpSuitSelected(event.matchImpl, trySuit, event.remaining_players, event.firstRound, event.player)
    }
    Some(true)
  }

  private def showcurtrevmet(event: ShowCurrentTrickEvent): Option[Boolean] = {
    latestOutput = ""
    val sb = new StringBuilder()
    sb.append("Current Trick:\n")
    sb.append("Trump-Suit: " + event.round.trumpSuit + "\n")
    if (event.trick.firstCard.isDefined) {
      sb.append(s"Suit to play: ${event.trick.firstCard.get.suit}\n")
    }
    for ((card, player) <- event.trick.cards) {
      sb.append(s"${player.name} played ${card.toString}\n")
    }
    latestOutput += sb.toString()
    //println(sb.toString())
    Some(true)
  }

  private val isInIO: AtomicBoolean = new AtomicBoolean(false)
  private val interrupted: AtomicBoolean = new AtomicBoolean(false)

  private def input(): String = {
    interrupted.set(false)
    val reader = new BufferedReader(new InputStreamReader(System.in))

    while (!interrupted.get()) {
      if (reader.ready()) {
        val in = reader.readLine()
        if (in.equals("undo")) {
          UndoManager.undoStep()
          throw new UndoneException("Undo")
        } else if (in.equals("redo")) {
          UndoManager.redoStep()
          throw new UndoneException("Redo")
        } else if (in.equals("load")
          && KnockOutWhist.config.persistenceManager.canLoadfile("currentSnapshot")) {
          KnockOutWhist.config.persistenceManager.loadFile("currentSnapshot.json")
          throw new UndoneException("Load")
        } else if (in.equals("save")) {
          KnockOutWhist.config.persistenceManager.saveFile("currentSnapshot.json")
        }
        return in
      }
      Thread.sleep(50)
    }
    throw new UndoneException("Skipped")
  }
}


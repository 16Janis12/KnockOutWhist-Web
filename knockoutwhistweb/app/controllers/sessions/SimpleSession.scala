package controllers.sessions

import de.knockoutwhist.cards.{Card, CardValue, Hand}
import de.knockoutwhist.events.ERROR_STATUS.*
import de.knockoutwhist.events.GLOBAL_STATUS.*
import de.knockoutwhist.events.PLAYER_STATUS.*
import de.knockoutwhist.events.ROUND_STATUS.*
import de.knockoutwhist.events.{ShowErrorStatus, ShowGlobalStatus, ShowPlayerStatus, ShowRoundStatus}
import de.knockoutwhist.events.cards.{RenderHandEvent, ShowTieCardsEvent}
import de.knockoutwhist.events.round.ShowCurrentTrickEvent
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.utils.events.SimpleEvent
import scalafx.scene.image.Image
import util.WebUIUtils

import java.util.UUID

case class SimpleSession(id: UUID, private var output: String) extends PlayerSession {
  def get(): String = {
    output
  }

  override def updatePlayer(event: SimpleEvent): Unit = {
    event match {
      case event: RenderHandEvent =>
        renderHand(event)
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
      case event: ShowCurrentTrickEvent =>
        showcurtrevmet(event)
    }
  }

  private def clear(): Unit = {
    output = ""
  }

  private def renderHand(event: RenderHandEvent): Unit = {
    output += WebUICards.renderHandEvent(event.hand, event.showNumbers).map(_.toString).mkString
  }

  private def showtiecardseventmethod(event: ShowTieCardsEvent): Option[Boolean] = {
    val a: Array[String] = Array("", "", "", "", "", "", "", "")
    for ((player, card) <- event.card) {
      val playerNameLength = player.name.length
      a(0) += " " + player.name + ":" + (" " * (playerNameLength - 1))
      val rendered = WebUIUtils.cardtoImage(card)
      //a(1) += " " + rendered(0)
      //a(2) += " " + rendered(1)
      //a(3) += " " + rendered(2)
      //a(4) += " " + rendered(3)
      //a(5) += " " + rendered(4)
      //a(6) += " " + rendered(5)
      //a(7) += " " + rendered(6)
    }
    a.foreach(addToOutput)
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
        clear()
        println("Starting a new match...")
        output += "\n\n"
        Some(true)
      case SHOW_TYPE_PLAYERS =>
        println("Please enter the names of the players, separated by a comma.")
        Some(true)
      case SHOW_FINISHED_MATCH =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[AbstractPlayer]) {
          None
        } else {
          clear()
          println(s"The match is over. The winner is ${event.objects.head.asInstanceOf[AbstractPlayer]}")
          Some(true)
        }
    }
  }

  private def showplayerstatusmethod(event: ShowPlayerStatus): Option[Boolean] = {
    val player = event.player
    event.status match {
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
        output = "\n\n"
        Some(true)
    }
  }

  private def showroundstatusmethod(event: ShowRoundStatus): Option[Boolean] = {
    event.status match {
      case SHOW_TURN =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[AbstractPlayer]) {
          None
        } else {
          println(s"It's ${event.objects.head.asInstanceOf[AbstractPlayer].name} turn.")
          Some(true)
        }
      case SHOW_START_ROUND =>
        clear()
        println(s"Starting a new round. The trump suit is ${event.currentRound.trumpSuit}.")
        output = "\n\n"
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
        println("Please enter a valid input")
        Some(true)
      case INVALID_NUMBER_OF_PLAYERS =>
        println("Please enter at least two names.")
        Some(true)
      case IDENTICAL_NAMES =>
        println("Please enter unique names.")
        Some(true)
      case INVALID_NAME_FORMAT =>
        println("Please enter valid names. Those can not be empty, shorter than 2 or longer then 10 characters.")
        Some(true)
      case WRONG_CARD =>
        if (event.objects.length != 1 || !event.objects.head.isInstanceOf[Card]) {
          None
        } else {
          println(f"You have to play a card of suit: ${event.objects.head.asInstanceOf[Card].suit}\n")
          Some(true)
        }
    }
  }

  private def showcurtrevmet(event: ShowCurrentTrickEvent): Option[Boolean] = {
    clear()
    val sb = new StringBuilder()
    sb.append("Current Trick:\n")
    sb.append("Trump-Suit: " + event.round.trumpSuit + "\n")
    if (event.trick.firstCard.isDefined) {
      sb.append(s"Suit to play: ${event.trick.firstCard.get.suit}\n")
    }
    for ((card, player) <- event.trick.cards) {
      sb.append(s"${player.name} played ${card.toString}\n")
    }
    println(sb.toString())
    Some(true)
  }

  private def addToOutput(str: String): Unit = {
    output += str + "\n"
  }

  private def println(s: String): Unit = {
    output += s + "\n"
  }

  private def println(): Unit = {
    output += "\n"
  }

  object WebUICards {
    def renderCardAsString(card: Card): Vector[String] = {
      val lines = "│         │"
      if (card.cardValue == CardValue.Ten) {
        return Vector(
          s"┌─────────┐",
          s"│${card.cardValue.cardType()}       │",
          lines,
          s"│    ${card.suit.cardType()}    │",
          lines,
          s"│       ${card.cardValue.cardType()}│",
          s"└─────────┘"
        )
      }
      Vector(
        s"┌─────────┐",
        s"│${card.cardValue.cardType()}        │",
        lines,
        s"│    ${card.suit.cardType()}    │",
        lines,
        s"│        ${card.cardValue.cardType()}│",
        s"└─────────┘"
      )
    }

    def renderHandEvent(hand: Hand, showNumbers: Boolean): List[Image] = {
      hand.cards.map(WebUIUtils.cardtoImage)
      //var zipped = cardStrings.transpose
      //if (showNumbers) zipped = {
      // List.tabulate(hand.cards.length) { i =>
      //    s"     ${i + 1}     "
      //  }
      // } :: zipped
      // zipped.map(_.mkString(" ")).toVector
      // }
    }
  }
}
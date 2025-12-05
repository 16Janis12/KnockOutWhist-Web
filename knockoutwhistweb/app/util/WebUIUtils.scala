package util

import de.knockoutwhist.cards.CardValue.*
import de.knockoutwhist.cards.Suit.{Clubs, Diamonds, Hearts, Spades}
import de.knockoutwhist.cards.{Card, Hand}
import play.api.libs.json.{JsArray, Json}
import play.twirl.api.Html
import scalafx.scene.image.Image

object WebUIUtils {
  def cardtoImage(card: Card): Html = {
    views.html.render.card.apply(cardToPath(card))(card.toString)
  }
  
  def cardToPath(card: Card): String = {
    f"images/cards/${cardtoString(card)}.png"
  }

  def cardtoString(card: Card): String = {
    val s = card.suit match {
      case Spades => "S"
      case Hearts => "H"
      case Clubs => "C"
      case Diamonds => "D"
    }
    val cv = card.cardValue match {
      case Ace => "A"
      case King => "K"
      case Queen => "Q"
      case Jack => "J"
      case Ten => "T"
      case Nine => "9"
      case Eight => "8"
      case Seven => "7"
      case Six => "6"
      case Five => "5"
      case Four => "4"
      case Three => "3"
      case Two => "2"
    }
    f"$cv$s"
  }
  
  def stringToCard(cardStr: String): Card = {
    val cv = cardStr.charAt(0) match {
      case 'A' => Ace
      case 'K' => King
      case 'Q' => Queen
      case 'J' => Jack
      case 'T' => Ten
      case '9' => Nine
      case '8' => Eight
      case '7' => Seven
      case '6' => Six
      case '5' => Five
      case '4' => Four
      case '3' => Three
      case '2' => Two
    }
    val s = cardStr.charAt(1) match {
      case 'S' => Spades
      case 'H' => Hearts
      case 'C' => Clubs
      case 'D' => Diamonds
    }
    Card(cv, s)
  }

  /**
   * Map a Hand to a JsArray of cards
   * Per card it has the string and the index in the hand
   * @param hand
   * @return
   */
  def handToJson(hand: Hand): JsArray = {
    val cards = hand.cards
    JsArray(
      cards.zipWithIndex.map { case (card, index) =>
        Json.obj(
          "idx" -> index,
          "card" -> cardtoString(card)
        )
      }
    )
  }

}

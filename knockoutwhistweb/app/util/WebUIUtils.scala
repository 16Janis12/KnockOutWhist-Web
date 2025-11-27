package util

import de.knockoutwhist.cards.CardValue.*
import de.knockoutwhist.cards.Suit.{Clubs, Diamonds, Hearts, Spades}
import de.knockoutwhist.cards.{Card, Hand}
import play.api.libs.json.{JsArray, Json}
import play.twirl.api.Html
import scalafx.scene.image.Image

object WebUIUtils {
  def cardtoImage(card: Card): Html = {
    views.html.render.card.apply(f"images/cards/${cardtoString(card)}.png")(card.toString)
  }

  def cardtoString(card: Card) = {
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

package util

import de.knockoutwhist.cards.Card
import de.knockoutwhist.cards.CardValue.{Ace, Eight, Five, Four, Jack, King, Nine, Queen, Seven, Six, Ten, Three, Two}
import de.knockoutwhist.cards.Suit.{Clubs, Diamonds, Hearts, Spades}
import scalafx.scene.image.Image

object WebUIUtils {
  def cardtoImage(card: Card): Image = {
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
    new Image(f"public/images/cards/$cv$s.png")
  }
}

package controllers.sessions

import de.knockoutwhist.cards.{Card, CardValue, Hand}
import de.knockoutwhist.player.AbstractPlayer
import de.knockoutwhist.utils.events.SimpleEvent
import play.twirl.api.Html
import scalafx.scene.image.Image
import util.WebUIUtils

import java.util.UUID

case class SimpleSession(id: UUID, private var output: List[Html]) extends PlayerSession {
  def get(): List[Html] = {
    output
  }

  override def updatePlayer(event: SimpleEvent): Unit = {
  }
}
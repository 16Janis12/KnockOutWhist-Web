package controllers.sessions

import de.knockoutwhist.rounds.Match
import de.knockoutwhist.rounds.Round
import de.knockoutwhist.utils.events.SimpleEvent
import de.knockoutwhist.player.AbstractPlayer
import java.util.UUID

case class AdvancedSession(player: AbstractPlayer) extends PlayerSession {
  override def id(): UUID = {
    player.id
  }
}

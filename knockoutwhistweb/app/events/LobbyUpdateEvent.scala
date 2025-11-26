package events

import de.knockoutwhist.utils.events.SimpleEvent

case class LobbyUpdateEvent() extends SimpleEvent {

  override def id: String = "LobbyUpdateEvent"
  
}

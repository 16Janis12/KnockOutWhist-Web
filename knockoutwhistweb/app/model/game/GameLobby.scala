package model.game

import de.knockoutwhist.control.GameLogic
import de.knockoutwhist.utils.events.{EventListener, SimpleEvent}

class GameLobby(val logic: GameLogic) extends EventListener{
  logic.addListener(this)
  logic.createSession()


  override def listen(event: SimpleEvent): Unit = {
    
  }
}

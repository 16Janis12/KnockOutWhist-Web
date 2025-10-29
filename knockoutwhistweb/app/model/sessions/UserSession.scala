package model.sessions

import de.knockoutwhist.player.AbstractPlayer
import java.util.UUID

class UserSession(id: UUID, player: AbstractPlayer) extends SimpleSession(id, player) {

}

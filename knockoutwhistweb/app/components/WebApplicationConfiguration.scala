package components

import de.knockoutwhist.components.DefaultConfiguration
import controllers.Gamesession
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.events.EventListener

class WebApplicationConfiguration extends DefaultConfiguration {

  override def uis: Set[UI] = super.uis + Gamesession
  override def listener: Set[EventListener] = super.listener + Gamesession

}

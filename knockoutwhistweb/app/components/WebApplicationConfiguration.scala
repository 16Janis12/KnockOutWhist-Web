package components

import de.knockoutwhist.components.DefaultConfiguration
import controllers.WebUI
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.events.EventListener

class WebApplicationConfiguration extends DefaultConfiguration {

  override def uis: Set[UI] = super.uis + WebUI
  override def listener: Set[EventListener] = super.listener + WebUI

}

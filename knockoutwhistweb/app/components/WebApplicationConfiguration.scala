package components

import de.knockoutwhist.components.DefaultConfiguration
import de.knockoutwhist.ui.UI
import de.knockoutwhist.utils.events.EventListener

class WebApplicationConfiguration extends DefaultConfiguration {

  override def uis: Set[UI] = Set()
  override def listener: Set[EventListener] = Set()

}

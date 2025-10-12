package di

import com.google.inject.AbstractModule
import components.WebApplicationConfiguration
import de.knockoutwhist.components.Configuration

class KnockOutWebConfigurationModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Configuration]).to(classOf[WebApplicationConfiguration])
  }

}
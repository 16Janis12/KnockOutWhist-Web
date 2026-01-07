package modules

import com.google.inject.AbstractModule
import logic.Gateway

class GatewayModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Gateway]).asEagerSingleton()
  }
}

package modules

import com.google.inject.AbstractModule

class GatewayModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[GatewayModule]).asEagerSingleton()
  }
}

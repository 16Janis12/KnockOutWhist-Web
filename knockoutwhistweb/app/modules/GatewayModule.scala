package modules

import com.google.inject.AbstractModule
import di.EntityManagerProvider
import jakarta.persistence.EntityManager
import logic.Gateway
import logic.user.UserManager
import logic.user.impl.HibernateUserManager

class GatewayModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Gateway]).asEagerSingleton()

    // Bind HibernateUserManager for production (when GatewayModule is used)
    bind(classOf[UserManager])
      .to(classOf[HibernateUserManager])
      .asEagerSingleton()

    // Bind EntityManager for JPA
    bind(classOf[EntityManager])
      .toProvider(classOf[EntityManagerProvider])
      .asEagerSingleton()
  }
}

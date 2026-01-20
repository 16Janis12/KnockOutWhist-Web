package di

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import logic.user.impl.HibernateUserManager
import play.api.db.DBApi
import play.api.{Configuration, Environment}

class ProductionModule(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {
    // Bind HibernateUserManager for production
    bind(classOf[logic.user.UserManager])
      .to(classOf[logic.user.impl.HibernateUserManager])
      .asEagerSingleton()

    // Bind EntityManager for JPA
    bind(classOf[jakarta.persistence.EntityManager])
      .toProvider(classOf[EntityManagerProvider])
      .asEagerSingleton()
  }
}

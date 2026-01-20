package di

import com.google.inject.Provider
import com.google.inject.Inject
import play.api.Configuration
import jakarta.inject.Singleton
import jakarta.persistence.{EntityManager, EntityManagerFactory, Persistence}

@Singleton
class EntityManagerProvider @Inject()(config: Configuration) extends Provider[EntityManager] {

  private val emf: EntityManagerFactory = {
    val dbConfig = config.get[Configuration]("db.default")
    val props = new java.util.HashMap[String, Object]()
    
    // Map Play configuration to Jakarta Persistence properties
    props.put("jakarta.persistence.jdbc.driver", dbConfig.get[String]("driver"))
    props.put("jakarta.persistence.jdbc.url", dbConfig.get[String]("url"))
    props.put("jakarta.persistence.jdbc.user", dbConfig.get[String]("username"))
    props.put("jakarta.persistence.jdbc.password", dbConfig.get[String]("password"))

    Persistence.createEntityManagerFactory("defaultPersistenceUnit", props)
  }

  override def get(): EntityManager = {
    emf.createEntityManager()
  }

  def close(): Unit = {
    if (emf.isOpen) {
      emf.close()
    }
  }
}

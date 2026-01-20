package di

import com.google.inject.Provider
import com.google.inject.Inject
import jakarta.inject.Singleton
import jakarta.persistence.{EntityManager, EntityManagerFactory, Persistence}

@Singleton
class EntityManagerProvider @Inject()() extends Provider[EntityManager] {

  private val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("defaultPersistenceUnit")

  override def get(): EntityManager = {
    emf.createEntityManager()
  }

  def close(): Unit = {
    if (emf.isOpen) {
      emf.close()
    }
  }
}

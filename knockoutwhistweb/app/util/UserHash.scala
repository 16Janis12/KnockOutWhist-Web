package util

import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import model.users.User

object UserHash {
  private val ITERATIONS: Int = 3
  private val MEMORY: Int = 32_768
  private val PARALLELISM: Int = 1
  private val SALT_LENGTH: Int = 32
  private val HASH_LENGTH: Int = 64
  private val ARGON_2 = Argon2Factory.create(Argon2Types.ARGON2id, SALT_LENGTH, HASH_LENGTH)

  def hashPW(password: String): String = {
    ARGON_2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray)
  }

  def verifyUser(password: String, user: User): Boolean = {
    ARGON_2.verify(user.passwordHash, password.toCharArray)
  }

}

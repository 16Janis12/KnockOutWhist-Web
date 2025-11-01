package model.users

import java.util.UUID

case class User(
               internalId: Long,
               id: UUID,
               name: String,
               passwordHash: String
               ) {

  def withName(newName: String): User = {
    this.copy(name = newName)
  }

  private def withPasswordHash(newPasswordHash: String): User = {
    this.copy(passwordHash = newPasswordHash)
  }

}

package model.users

import jakarta.persistence.*

import java.time.LocalDateTime
import java.util.UUID
import scala.compiletime.uninitialized

@Entity
@Table(name = "users")
class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = uninitialized

  @Column(name = "uuid", nullable = false, unique = true)
  var uuid: UUID = uninitialized

  @Column(name = "username", nullable = false, unique = true)
  var username: String = uninitialized

  @Column(name = "password_hash", nullable = false)
  var passwordHash: String = uninitialized

  @Column(name = "openid_provider")
  var openidProvider: String = uninitialized

  @Column(name = "openid_provider_id")
  var openidProviderId: String = uninitialized

  @Column(name = "created_at", nullable = false)
  var createdAt: LocalDateTime = uninitialized

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = uninitialized

  @PrePersist
  def onCreate(): Unit = {
    val now = LocalDateTime.now()
    createdAt = now
    updatedAt = now
    if (uuid == null) {
      uuid = UUID.randomUUID()
    }
  }

  @PreUpdate
  def onUpdate(): Unit = {
    updatedAt = LocalDateTime.now()
  }

  def toUser: User = {
    User(
      internalId = id,
      id = uuid,
      name = username,
      passwordHash = passwordHash
    )
  }
}

object UserEntity {
  def fromUser(user: User): UserEntity = {
    val entity = new UserEntity()
    entity.uuid = user.id
    entity.username = user.name
    entity.passwordHash = user.passwordHash
    entity
  }

  def fromOpenIDUser(username: String, userInfo: services.OpenIDUserInfo): UserEntity = {
    val entity = new UserEntity()
    entity.username = username
    entity.passwordHash = "" // No password for OpenID users
    entity.openidProvider = userInfo.provider
    entity.openidProviderId = userInfo.id
    entity
  }
}

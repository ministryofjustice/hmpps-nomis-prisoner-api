package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import org.hibernate.Hibernate

@Embeddable
data class ProfileCodeId(
  @Column(name = "PROFILE_TYPE")
  val type: String,

  @Column(name = "PROFILE_CODE")
  val code: String,
)

@Entity(name = "PROFILE_CODES")
class ProfileCode(
  @EmbeddedId
  @Column
  val id: ProfileCodeId,

  @Column
  val description: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ProfileCode

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(profileType = $id.profileType, profileCode = $id.profileCode)"
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable

@Embeddable
class MovementTypeAndReasonId(
  @Column(name = "MOVEMENT_TYPE", updatable = false, insertable = false)
  val type: String,

  @Column(name = "MOVEMENT_REASON_CODE", updatable = false, insertable = false)
  val reasonCode: String,
) : Serializable

@Entity
@Table(name = "MOVEMENT_REASONS")
class MovementTypeAndReason(
  @EmbeddedId
  val id: MovementTypeAndReasonId,

  val description: String,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as MovementTypeAndReason
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String = "MovementTypeAndReason(id=$id, description='$description')"
}

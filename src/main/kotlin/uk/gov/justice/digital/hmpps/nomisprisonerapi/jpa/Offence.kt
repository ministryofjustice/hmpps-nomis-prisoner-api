package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable

@Embeddable
class OffenceId(
  @Column(name = "OFFENCE_CODE", nullable = false)
  val offenceCode: String,
  @Column(name = "STATUTE_CODE", nullable = false)
  val statuteCode: String,
) : Serializable

@Entity
@Table(name = "OFFENCES")
@EntityOpen
class Offence(
  @EmbeddedId
  val id: OffenceId,
  val description: String,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Offence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String {
    return "Offence(id=$id, description='$description')"
  }
}

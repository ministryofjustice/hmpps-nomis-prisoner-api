package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.util.Objects

@Entity
@Table(name = "STATUTES")
@EntityOpen
class Statute(
  @Id
  @Column(name = "STATUTE_CODE")
  val code: String,

  val description: String,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Statute
    return code == other.code
  }

  override fun hashCode(): Int = Objects.hashCode(code)
}

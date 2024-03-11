package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.util.Objects

@Entity
@Table(name = "INCIDENT_STATUSES")
class IncidentStatus(
  @Id
  val code: String,

  val description: String,

  @Column(name = "LIST_SEQ")
  val listSequence: Int? = 1,

  @Column(name = "STANDARD_USER_FLAG")
  @Convert(converter = YesNoConverter::class)
  val standardUser: Boolean = false,

  @Column(name = "ENHANCED_USER_FLAG")
  @Convert(converter = YesNoConverter::class)
  val enhancedUser: Boolean = false,

  // active flag - did not map as all Y = true
  // expiry date - did not map as all null
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenceResultCode
    return code == other.code
  }

  override fun hashCode(): Int {
    return Objects.hashCode(code)
  }
}

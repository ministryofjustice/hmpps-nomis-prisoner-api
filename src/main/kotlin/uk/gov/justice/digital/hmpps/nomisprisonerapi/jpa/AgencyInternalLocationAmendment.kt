package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime
import java.util.Objects

@Entity
@Table(name = "AGY_INT_LOC_AMENDMENTS")
@EntityOpen
data class AgencyInternalLocationAmendment(
  @Id
  @SequenceGenerator(name = "AGY_INT_LOC_AMENDMENT_ID", sequenceName = "AGY_INT_LOC_AMENDMENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "AGY_INT_LOC_AMENDMENT_ID")
  @Column(name = "AGY_INT_LOC_AMENDMENT_ID")
  val amendmentId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID", nullable = false)
  val agencyInternalLocation: AgencyInternalLocation,

  @Column(name = "AMEND_DATE")
  val amendDateTime: LocalDateTime,

  val columnName: String? = null,
  val oldValue: String? = null,
  val newValue: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${LivingUnitReason.LIV_UN_RSN}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "DEACTIVATE_REASON_CODE", referencedColumnName = "code", nullable = true),
      ),
    ],
  )
  val deactivateReason: LivingUnitReason? = null,

  val actionCode: String? = null,
  val amendUserId: String,

  // INT_LOC_PROFILE_CODE column: not used
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyInternalLocationAmendment
    return amendmentId == other.amendmentId
  }

  override fun hashCode(): Int = Objects.hashCode(amendmentId)

  override fun toString(): String =
    "AgencyInternalLocationAmendment(amendmentId=$amendmentId, amendDateTime=$amendDateTime, columnName=$columnName, oldValue=$oldValue, newValue=$newValue, amendUserId=$amendUserId)"
}

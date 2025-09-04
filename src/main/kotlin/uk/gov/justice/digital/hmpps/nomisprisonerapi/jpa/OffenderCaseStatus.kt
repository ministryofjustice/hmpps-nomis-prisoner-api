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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@Table(name = "OFFENDER_CASE_STATUSES")
@EntityOpen
class OffenderCaseStatus(

  @SequenceGenerator(
    name = "OFFENDER_CASE_STATUS_ID",
    sequenceName = "OFFENDER_CASE_STATUS_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_CASE_STATUS_ID")
  @Id
  @Column(name = "OFFENDER_CASE_STATUS_ID")
  val id: Long = 0,

  val statusUpdateReason: String,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase,

  @Column(name = "STATUS_UPDATE_STAFF_ID")
  val staffId: Long,

  // this class is only mapped to allow a cascading delete from OFFENDER_CASES
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderCaseStatus
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

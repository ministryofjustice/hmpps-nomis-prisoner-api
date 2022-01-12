package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VISIT_BALANCES")
data class OffenderVisitBalance(

  @Id
  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBookingId: Long,

  @Column(name = "REMAINING_VO")
  val remainingVisitOrders: Int? = null,

  @Column(name = "REMAINING_PVO")
  val remainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "VISIT_ALLOWANCE_INDICATOR")
  val visitAllowanceIndicator: Boolean? = false,
) : AuditableEntity()

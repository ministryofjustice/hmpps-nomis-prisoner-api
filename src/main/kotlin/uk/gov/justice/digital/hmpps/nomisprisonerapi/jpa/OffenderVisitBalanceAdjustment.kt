package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.validation.constraints.Size

@Entity
@Table(name = "OFFENDER_VISIT_BALANCE_ADJS")
data class OffenderVisitBalanceAdjustment(
  @SequenceGenerator(name = "OFFENDER_VISIT_BALANCE_ADJ_ID", sequenceName = "OFFENDER_VISIT_BALANCE_ADJ_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_VISIT_BALANCE_ADJ_ID")
  @Id
  @Column(name = "OFFENDER_VISIT_BALANCE_ADJ_ID")
  val id: Long? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "REMAINING_VO")
  val remainingVisitOrders: Int? = null,

  @Column(name = "PREVIOUS_REMAINING_VO")
  val previousRemainingVisitOrders: Int? = null,

  @Column(name = "REMAINING_PVO")
  val remainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "PREVIOUS_REMAINING_PVO")
  val previousRemainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "ADJUST_REASON_CODE")
  @Size(max = 12)
  val adjustReasonCode: String? = null,

  @Column(name = "AUTHORISED_STAFF_ID")
  val authorisedStaffId: Long? = null,

  @Column(name = "ENDORSED_STAFF_ID")
  val endorsedStaffId: Long? = null,

  @Column(name = "ADJUST_DATE")
  val adjustDate: LocalDate? = null,

  @Column(name = "COMMENT_TEXT")
  @Size(max = 240)
  val commentText: String? = null,

  @Column(name = "EXPIRY_BALANCE")
  val expiryBalance: Int? = null,

  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate? = null,

  @Column(name = "EXPIRY_STATUS")
  @Size(max = 3)
  val expiryStatus: String? = null
) : AuditableEntity()

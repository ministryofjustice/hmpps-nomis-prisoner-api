package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate

@EntityOpen
@Entity
@Table(name = "OFFENDER_IND_SCH_WAIT_LISTS")
class OffenderTransferScheduleWaitList(
  @Id
  @Column(name = "EVENT_ID")
  var id: Long = 0,

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "EVENT_ID")
  var schedule: OffenderTransferScheduleOut,

  @Column(name = "REQUEST_DATE")
  var requestDate: LocalDate,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TransferScheduleStatus.TRN_SCH_STS}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "WAIT_LIST_STATUS", referencedColumnName = "code")),
    ],
  )
  var waitListStatus: TransferScheduleStatus,

  @Column(name = "STATUS_DATE")
  var statusDate: LocalDate,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TransferPriority.TRN_PRIORITY}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TRANSFER_PRIORITY", referencedColumnName = "code")),
    ],
  )
  // This column is not actually nullable but there are some records with invalid values,
  // therefore we treat as nullable and default in the service layer if null
  @NotFound(action = NotFoundAction.IGNORE)
  var transferPriority: TransferPriority? = null,

  @Column(name = "APPROVED_FLAG")
  @Convert(converter = YesNoConverter::class)
  var approvedFlag: Boolean = false,

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "APPROVED_STAFF_ID")
  @NotFound(action = NotFoundAction.IGNORE)
  var approvedStaff: Staff? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TransferCancellationReason.TRANSFER_CANCELLATION_REASON}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OUTCOME_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var cancellationReasonCode: TransferCancellationReason? = null,

  @Column(name = "COMMENT_TEXT_1")
  var commentText1: String? = null,

  @Column(name = "COMMENT_TEXT_2")
  var commentText2: String? = null,
) : NomisAuditableEntityWithStaff() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTransferScheduleWaitList

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id)"
}

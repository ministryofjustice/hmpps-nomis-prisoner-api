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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_SENT_CALCULATIONS")
@EntityOpen
class OffenderSentenceCalculation(
  @SequenceGenerator(
    name = "OFFENDER_SENT_CALCULATION_ID",
    sequenceName = "OFFENDER_SENT_CALCULATION_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_SENT_CALCULATION_ID")
  @Id
  @Column(name = "OFFENDER_SENT_CALCULATION_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "CALCULATION_DATE")
  var calculationDate: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STAFF_ID")
  var staff: Staff? = null,

  @Column(name = "CALC_REASON_CODE")
  var calculationReasonCode: String,

  @Column(name = "COMMENT_TEXT")
  var commentText: String? = null,

  @Column(name = "EFFECTIVE_SENTENCE_END_DATE")
  var effectiveSentenceEndDate: LocalDate? = null,

  // format 'YY/MM/DD'
  @Column(name = "EFFECTIVE_SENTENCE_LENGTH")
  var effectiveSentenceLength: String? = null,

  // format 'YY/MM/DD'
  @Column(name = "JUDICIALLY_IMPOSED_SENT_LENGTH")
  var judiciallyImposedSentenceLength: String? = null,

  @Column(name = "WORKFLOW_HISTORY_ID")
  var workflowHistoryId: Long? = null,

  @Column(name = "HDCED_CALCULATED_DATE")
  var hdcedCalculatedDate: LocalDate? = null,

  @Column(name = "HDCED_OVERRIDED_DATE")
  var hdcedOverridedDate: LocalDate? = null,

  @Column(name = "HDCAD_CALCULATED_DATE")
  var hdcadCalculatedDate: LocalDate? = null,

  @Column(name = "HDCAD_OVERRIDED_DATE")
  var hdcadOverridedDate: LocalDate? = null,

  @Column(name = "ETD_CALCULATED_DATE")
  var etdCalculatedDate: LocalDate? = null,

  @Column(name = "ETD_OVERRIDED_DATE")
  var etdOverridedDate: LocalDate? = null,

  @Column(name = "MTD_CALCULATED_DATE")
  var mtdCalculatedDate: LocalDate? = null,

  @Column(name = "MTD_OVERRIDED_DATE")
  var mtdOverridedDate: LocalDate? = null,

  @Column(name = "LTD_CALCULATED_DATE")
  var ltdCalculatedDate: LocalDate? = null,

  @Column(name = "LTD_OVERRIDED_DATE")
  var ltdOverridedDate: LocalDate? = null,

  @Column(name = "ARD_CALCULATED_DATE")
  var ardCalculatedDate: LocalDate? = null,

  @Column(name = "ARD_OVERRIDED_DATE")
  var ardOverridedDate: LocalDate? = null,

  @Column(name = "CRD_CALCULATED_DATE")
  var crdCalculatedDate: LocalDate? = null,

  @Column(name = "CRD_OVERRIDED_DATE")
  var crdOverridedDate: LocalDate? = null,

  @Column(name = "PED_CALCULATED_DATE")
  var pedCalculatedDate: LocalDate? = null,

  @Column(name = "PED_OVERRIDED_DATE")
  var pedOverridedDate: LocalDate? = null,

  @Column(name = "APD_CALCULATED_DATE")
  var apdCalculatedDate: LocalDate? = null,

  @Column(name = "APD_OVERRIDED_DATE")
  var apdOverridedDate: LocalDate? = null,

  @Column(name = "NPD_CALCULATED_DATE")
  var npdCalculatedDate: LocalDate? = null,

  @Column(name = "NPD_OVERRIDED_DATE")
  var npdOverridedDate: LocalDate? = null,

  @Column(name = "LED_CALCULATED_DATE")
  var ledCalculatedDate: LocalDate? = null,

  @Column(name = "LED_OVERRIDED_DATE")
  var ledOverridedDate: LocalDate? = null,

  @Column(name = "SED_CALCULATED_DATE")
  var sedCalculatedDate: LocalDate? = null,

  @Column(name = "SED_OVERRIDED_DATE")
  var sedOverridedDate: LocalDate? = null,

  @Column(name = "PRRD_CALCULATED_DATE")
  var prrdCalculatedDate: LocalDate? = null,

  @Column(name = "PRRD_OVERRIDED_DATE")
  var prrdOverridedDate: LocalDate? = null,

  @Column(name = "TARIFF_CALCULATED_DATE")
  var tariffCalculatedDate: LocalDate? = null,

  @Column(name = "TARIFF_OVERRIDED_DATE")
  var tariffOverridedDate: LocalDate? = null,

  @Column(name = "DPRRD_CALCULATED_DATE")
  var dprrdCalculatedDate: LocalDate? = null,

  @Column(name = "DPRRD_OVERRIDED_DATE")
  var dprrdOverridedDate: LocalDate? = null,

  @Column(name = "ERSED_OVERRIDED_DATE")
  var ersedOverridedDate: LocalDate? = null,

  @Column(name = "TERSED_OVERRIDED_DATE")
  var tersedOverridedDate: LocalDate? = null,

  @Column(name = "ROTL_OVERRIDED_DATE")
  var rotlOverridedDate: LocalDate? = null,

  @Column(name = "TUSED_CALCULATED_DATE")
  var tusedCalculatedDate: LocalDate? = null,

  @Column(name = "TUSED_OVERRIDED_DATE")
  var tusedOverridedDate: LocalDate? = null,

  // The schema allows nulls for RECORD_DATETIME and RECORD_USER_ID (and CALCULATION_DATE, CALC_REASON_CODE),
  // but a check of NOMIS PREPROD (13.5M rows) found no null values, so they are mapped as non-null.
  @Column(name = "RECORD_DATETIME")
  var recordDatetime: LocalDateTime = LocalDateTime.now(),

  @Column(name = "RECORD_USER_ID")
  var recordUserId: String,

  /* COLUMNS NOT MAPPED
    HDC_ELIGIBLE_WF - not used since 2009, defaults to 'N'
   */
) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentenceCalculation
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

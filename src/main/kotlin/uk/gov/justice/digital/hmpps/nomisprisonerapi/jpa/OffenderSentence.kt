package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class SentenceId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sequence: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_SENTENCES")
data class OffenderSentence(
  @EmbeddedId
  val id: SentenceId,
  @OneToMany(mappedBy = "sentence", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val adjustments: MutableList<OffenderSentenceAdjustment> = mutableListOf(),

  // 'I' or 'A'
  @Column(name = "SENTENCE_STATUS")
  val status: String,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "SENTENCE_CALC_TYPE",
        referencedColumnName = "SENTENCE_CALC_TYPE",
      ),
      JoinColumn(name = "SENTENCE_CATEGORY", referencedColumnName = "SENTENCE_CATEGORY"),
    ],
  )
  @BatchSize(size = 25)
  val calculationType: SentenceCalculationType,

  @Column(name = "START_DATE")
  val startDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID")
  val courtOrder: CourtOrder? = null,

  @Column(name = "CONSEC_TO_SENTENCE_SEQ")
  val consecSequence: Int? = null,

  @Column(name = "END_DATE")
  val endDate: LocalDate? = null,

  val commentText: String? = null,

  // 49 rows in prod last used 2021
  @Column(name = "NO_OF_UNEXCUSED_ABSENCE")
  val absenceCount: Int? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase? = null,

  @Column(name = "ETD_CALCULATED_DATE")
  val etdCalculatedDate: LocalDate? = null,

  @Column(name = "MTD_CALCULATED_DATE")
  val mtdCalculatedDate: LocalDate? = null,

  @Column(name = "LTD_CALCULATED_DATE")
  val ltdCalculatedDate: LocalDate? = null,

  @Column(name = "ARD_CALCULATED_DATE")
  val ardCalculatedDate: LocalDate? = null,

  @Column(name = "CRD_CALCULATED_DATE")
  val crdCalculatedDate: LocalDate? = null,

  @Column(name = "PED_CALCULATED_DATE")
  val pedCalculatedDate: LocalDate? = null,

  @Column(name = "NPD_CALCULATED_DATE")
  val npdCalculatedDate: LocalDate? = null,

  @Column(name = "LED_CALCULATED_DATE")
  val ledCalculatedDate: LocalDate? = null,

  @Column(name = "SED_CALCULATED_DATE")
  val sedCalculatedDate: LocalDate? = null,

  @Column(name = "PRRD_CALCULATED_DATE")
  val prrdCalculatedDate: LocalDate? = null,

  @Column(name = "TARIFF_CALCULATED_DATE")
  val tariffCalculatedDate: LocalDate? = null,

  @Column(name = "DPRRD_CALCULATED_DATE")
  val dprrdCalculatedDate: LocalDate? = null,

  @Column(name = "TUSED_CALCULATED_DATE")
  val tusedCalculatedDate: LocalDate? = null,

  @Column(name = "AGG_SENTENCE_SEQ")
  val aggSentenceSequence: Int? = null,

  @Column(name = "AGGREGATE_ADJUST_DAYS")
  val aggAdjustDays: Int? = null,

  // 'IND' or 'AGG' in prod - defaults to 'IND'
  val sentenceLevel: String,

  val extendedDays: Int? = null,

  val counts: Int? = null,

  val statusUpdateReason: String? = null,

  val statusUpdateComment: String? = null,

  val statusUpdateDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STATUS_UPDATE_STAFF_ID")
  var statusUpdateStaff: Staff? = null,

  // optional on DB but no nulls in prod
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SentenceCategoryType.CATEGORY + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "SENTENCE_CATEGORY",
          referencedColumnName = "code",
          insertable = false,
          updatable = false,
        ),
      ),
    ],
  )
  val category: SentenceCategoryType,

  val fineAmount: BigDecimal? = null,

  val dischargeDate: LocalDate? = null,

  @Column(name = "NOMSENTDETAILREF")
  val nomSentDetailRef: Long? = null,

  @Column(name = "NOMCONSTOSENTDETAILREF")
  val nomConsToSentDetailRef: Long? = null,

  @Column(name = "NOMCONSFROMSENTDETAILREF")
  val nomConsFromSentDetailRef: Long? = null,

  @Column(name = "NOMCONCWITHSENTDETAILREF")
  val nomConsWithSentDetailRef: Long? = null,

  @Column(name = "LINE_SEQ")
  val lineSequence: Int? = null,

  @Convert(converter = YesNoConverter::class)
  val hdcExclusionFlag: Boolean? = false,

  val hdcExclusionReason: String? = null,

  val cjaAct: String? = null,

  @Column(name = "START_DATE_2CALC")
  val sled2Calc: LocalDate? = null,

  @Column(name = "SLED_2CALC")
  val startDate2Calc: LocalDate? = null,

  @OneToMany(mappedBy = "offenderSentence", cascade = [CascadeType.ALL], orphanRemoval = true)
  val offenderSentenceCharges: MutableList<OffenderSentenceCharge> = mutableListOf(),

  @OneToMany(mappedBy = "offenderSentence", cascade = [CascadeType.ALL], orphanRemoval = true)
  val offenderSentenceTerms: MutableList<OffenderSentenceTerm> = mutableListOf(),

  /* COLUMNS NOT MAPPED
    TERMINATION_REASON - not used
    TERMINATION_DATE - not used
    APD_CALCULATED_DATE - not used
    SENTENCE_TEXT - not used
    REVOKED_DATE - not used
    REVOKED_STAFF_ID - not used
    BREACH_LEVEL - not used
    TERMINATION_DATE - not used
    AGGREGATE_TERM - not used
    WORKFLOW_ID - not used
   */

) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

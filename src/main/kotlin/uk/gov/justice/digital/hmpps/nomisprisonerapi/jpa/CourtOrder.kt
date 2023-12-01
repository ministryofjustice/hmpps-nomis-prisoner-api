package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "ORDERS")
@EntityOpen
class CourtOrder(
  @SequenceGenerator(
    name = "ORDER_ID",
    sequenceName = "ORDER_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "ORDER_ID")
  @Id
  @Column(name = "ORDER_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  // optional on DB but no nulls in prod
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase,

  // optional on DB but no nulls in prod
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "EVENT_ID", nullable = false)
  var courtEvent: CourtEvent,

  val courtDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ISSUING_AGY_LOC_ID", nullable = false)
  val issuingCourt: AgencyLocation,

  // assigned by the court
  val courtInfoId: String?,

  // no reference code match: AUTO, NAR, REM, SEC 10/3, TRL
  val orderType: String,

  val orderStatus: String = "A", // always A in prod

  val dueDate: LocalDate?, // usually less than 10 a year

  // db comment domain is not correct
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SeriousnessLevelType.PSR_LEV_SER + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "ORDER_SERIOUSNESS_LEVEL", referencedColumnName = "code")),
    ],
  )
  val seriousnessLevel: SeriousnessLevelType?,

  val requestDate: LocalDate?, // usually less than 10 a year

  @Convert(converter = YesNoConverter::class)
  val nonReportFlag: Boolean?,

  val commentText: String?, // not used since 2018

  @OneToMany(mappedBy = "id.orderId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val sentencePurposes: MutableList<SentencePurpose> = mutableListOf(),

  /* COLUMNS NOT MAPPED
    COURT_SERIOUSNESS_LEVEL - not used
    STAFF_WORK_ID - not used
    COMPLETE_DATE - not used
    COMPLETE_STAFF_ID - not used
    INTERVENTION_TIER_CODE - not used
    CPS_RECEIVED_DATE - not used
    ISSUE_DATE - not used
    MESSAGE_ID - not used
    OFFENDER_PROCEEDING_ID - not used
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
    other as CourtOrder
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

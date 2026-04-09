package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_CHARGES")
@EntityOpen
class OffenderCharge(
  @SequenceGenerator(
    name = "OFFENDER_CHARGE_ID",
    sequenceName = "OFFENDER_CHARGE_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_CHARGE_ID")
  @Id
  @Column(name = "OFFENDER_CHARGE_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "OFFENCE_CODE",
        referencedColumnName = "OFFENCE_CODE",
      ),
      JoinColumn(
        // statute code
        name = "STATUTE_CODE",
        referencedColumnName = "STATUTE_CODE",
      ),
    ],
  )
  var offence: Offence,

  @Column(name = "NO_OF_OFFENCES")
  var offencesCount: Int? = null,

  var offenceDate: LocalDate? = null,

  @Column(name = "OFFENCE_RANGE_DATE")
  var offenceEndDate: LocalDate? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PleaStatusType.PLEA_STATUS + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "PLEA_CODE", referencedColumnName = "code")),
    ],
  )
  var plea: PleaStatusType? = null,

  val propertyValue: BigDecimal? = null,

  val totalPropertyValue: BigDecimal? = null,

  @Column(name = "CJIT_OFFENCE_CODE_1")
  val cjitCode1: String? = null,

  @Column(name = "CJIT_OFFENCE_CODE_2")
  val cjitCode2: String? = null,

  @Column(name = "CJIT_OFFENCE_CODE_3")
  val cjitCode3: String? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ChargeStatusType.CHARGE_STS + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "CHARGE_STATUS", referencedColumnName = "code")),
    ],
  )
  var chargeStatus: ChargeStatusType? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RESULT_CODE_1")
  var resultCode1: OffenceResultCode? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RESULT_CODE_2")
  val resultCode2: OffenceResultCode? = null,

  @Column(name = "RESULT_CODE_1_INDICATOR")
  var resultCode1Indicator: String? = null,

  @Column(name = "RESULT_CODE_2_INDICATOR")
  val resultCode2Indicator: String? = null,

  @Convert(converter = YesNoConverter::class)
  var mostSeriousFlag: Boolean = false,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  var courtCase: CourtCase,

  // always populated in prod but presumably won't be by DPS
  val lidsOffenceNumber: Int? = null,

  @OneToMany(mappedBy = "offenderCharge", cascade = [CascadeType.REMOVE])
  val offenderSentenceCharges: List<OffenderSentenceCharge> = mutableListOf(),

  /* COLUMNS NOT MAPPED
    CHARGE_SEQ - not used
    ORDER_ID - not used
   */

) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderCharge
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

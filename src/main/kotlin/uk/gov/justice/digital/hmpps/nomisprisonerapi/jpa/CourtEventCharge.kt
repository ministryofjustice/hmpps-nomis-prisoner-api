package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
class CourtEventChargeId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_CHARGE_ID", nullable = false)
  var offenderCharge: OffenderCharge,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "EVENT_ID", nullable = false)
  var courtEvent: CourtEvent,
) : Serializable

@Entity
@Table(name = "COURT_EVENT_CHARGES")
@EntityOpen
class CourtEventCharge(
  @EmbeddedId
  val id: CourtEventChargeId,

  @Column(name = "NO_OF_OFFENCES")
  val offencesCount: Int? = 0,

  val offenceDate: LocalDate?,

  @Column(name = "OFFENCE_RANGE_DATE")
  val offenceEndDate: LocalDate?,

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
  val plea: PleaStatusType?,

  val propertyValue: BigDecimal?,

  val totalPropertyValue: BigDecimal?,

  @Column(name = "CJIT_OFFENCE_CODE_1")
  val cjitCode1: String?,

  @Column(name = "CJIT_OFFENCE_CODE_2")
  val cjitCode2: String?,

  @Column(name = "CJIT_OFFENCE_CODE_3")
  val cjitCode3: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RESULT_CODE_1")
  val resultCode1: OffenceResultCode?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RESULT_CODE_2")
  val resultCode2: OffenceResultCode?,

  @Column(name = "RESULT_CODE_1_INDICATOR")
  val resultCode1Indicator: String?,

  @Column(name = "RESULT_CODE_2_INDICATOR")
  val resultCode2Indicator: String?,

  @Convert(converter = YesNoConverter::class)
  val mostSeriousFlag: Boolean,

) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourtEventCharge
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

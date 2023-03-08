package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Embeddable
data class CourseActivityPayRateId(

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  @Column(name = "IEP_LEVEL", nullable = false)
  val iepLevelCode: String,

  @Column(name = "PAY_BAND_CODE", nullable = false)
  val payBandCode: String,

  @Column(nullable = false)
  val startDate: LocalDate,
) : Serializable {

  fun toTelemetry() = "$iepLevelCode-$payBandCode-$startDate"
}

@Entity
@Table(name = "COURSE_ACTIVITY_PAY_RATES")
data class CourseActivityPayRate(

  @EmbeddedId
  val id: CourseActivityPayRateId,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PayBand.PAY_BAND + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PAY_BAND_CODE", referencedColumnName = "code", nullable = true, updatable = false, insertable = false)),
    ],
  )
  val payBand: PayBand,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IEPLevel.IEP_LEVEL + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "IEP_LEVEL", referencedColumnName = "code", nullable = true, updatable = false, insertable = false)),
    ],
  )
  val iepLevel: IEPLevel,

  @Column
  var endDate: LocalDate? = null,

  @Column(nullable = false)
  var halfDayRate: BigDecimal,

  @Column
  val commentText: String = "Copied from the DPS activities service",
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseActivityPayRate

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "CourseActivityPayRate(courseActivityId=${id.courseActivity.courseActivityId}, iepLevel=${id.iepLevelCode}, payBandCode=${id.payBandCode}, startDate=${id.startDate})"

  fun hasExpiryDate(): Boolean = endDate != null
  fun expire(): CourseActivityPayRate = this.apply { endDate = LocalDate.now() }
  fun hasFutureStartDate(): Boolean = id.startDate > LocalDate.now()

  companion object {
    fun preciseHalfDayRate(halfDayRate: BigDecimal): BigDecimal = halfDayRate.setScale(3, RoundingMode.HALF_UP)
  }
}

/*
also:
COMMENT_TEXT	VARCHAR2(240 CHAR)			Free text comment
 */

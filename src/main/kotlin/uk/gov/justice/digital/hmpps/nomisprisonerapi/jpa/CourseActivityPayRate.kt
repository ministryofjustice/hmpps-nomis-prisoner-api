package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Entity
@Table(name = "COURSE_ACTIVITY_PAY_RATES")
data class CourseActivityPayRate(

  @Id
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  @Id
  @Column(name = "IEP_LEVEL", nullable = false)
  val iepLevelCode: String,

  @Id
  @Column(nullable = false)
  val payBandCode: String,

  @Id
  @Column(nullable = false)
  val startDate: LocalDate,

  @Column
  var endDate: LocalDate? = null,

  @Column(nullable = false)
  var halfDayRate: BigDecimal,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseActivityPayRate

    return courseActivity.courseActivityId == other.courseActivity.courseActivityId &&
      iepLevelCode == other.iepLevelCode &&
      payBandCode == other.payBandCode &&
      startDate == other.startDate
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "CourseActivityPayRate(courseActivity=$courseActivity, iepLevel=$iepLevelCode, payBandCode=$payBandCode, startDate=$startDate)"

  fun isExpired(): Boolean = endDate != null
  fun expire(): CourseActivityPayRate = this.apply { endDate = LocalDate.now() }
  fun isNotYetActive(): Boolean = startDate > LocalDate.now()

  companion object {
    fun preciseHalfDayRate(halfDayRate: BigDecimal) = halfDayRate.setScale(3, RoundingMode.HALF_UP)
  }
}

/*
also:
COMMENT_TEXT	VARCHAR2(240 CHAR)			Free text comment
 */

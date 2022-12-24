package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.Size

@Entity
@Table(name = "COURSE_ACTIVITY_PAY_RATES")
data class CourseActivityPayRate(

  @Id
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  @Id
  @Column
  @Size(max = 6)
  var iepLevel: String? = null,

  @Id
  @Column
  @Size(max = 12)
  var payBandCode: String? = null,

  @Id
  @Column
  val startDate: LocalDate? = null,

  @Column
  val endDate: LocalDate? = null,

  @Column
  var halfDayRate: BigDecimal? = null,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseActivityPayRate

    return courseActivity.courseActivityId == other.courseActivity.courseActivityId &&
      iepLevel == other.iepLevel &&
      payBandCode == other.payBandCode &&
      startDate == other.startDate
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "CourseActivityPayRate(courseActivity=$courseActivity, iepLevel=$iepLevel, payBandCode=$payBandCode, startDate=$startDate)"
}

/*
also:
COMMENT_TEXT	VARCHAR2(240 CHAR)	Yes		Free text comment
 */

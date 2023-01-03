package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

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
  val endDate: LocalDate? = null,

//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumnsOrFormulas(
//    value = [
//      JoinColumnOrFormula(
//        formula = JoinFormula(
//          value = "'" + IEPLevel.IEP_LEVEL + "'",
//          referencedColumnName = "domain"
//        )
//      ),
//      JoinColumnOrFormula(column = JoinColumn(name = "IEP_LEVEL", referencedColumnName = "code"))
//    ]
//  )
//  val iepLevel: IEPLevel? = null,

  @Column(nullable = false)
  val halfDayRate: BigDecimal,
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
}

/*
also:
COMMENT_TEXT	VARCHAR2(240 CHAR)			Free text comment
 */

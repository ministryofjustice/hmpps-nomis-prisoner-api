package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable

@Entity
@Table(name = "COURSE_ACTIVITY_AREAS")
data class CourseActivityAreas(

  @Id
  @Column(name = "CRS_ACTY_ID", nullable = false)
  val courseActivityId: Long,

  @OneToOne
  @MapsId
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false, insertable = false, updatable = false)
  val courseActivity: CourseActivity,

  @Column(nullable = false)
  val areaCode: String,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseActivityAreas

    return courseActivityId == other.courseActivityId
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "CourseActivityArea(courseActivityId=$courseActivityId, areaCode=$areaCode)"
}

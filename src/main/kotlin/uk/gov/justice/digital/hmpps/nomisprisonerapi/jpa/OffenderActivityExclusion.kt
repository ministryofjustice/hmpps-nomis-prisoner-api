package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "OFFENDER_EXCLUDE_ACTS_SCHDS")
data class OffenderActivityExclusion(
  @Id
  @Column(name = "OFFENDER_EXCLUDE_ACT_SCHD_ID")
  @SequenceGenerator(name = "OFFENDER_EXCLUDE_ACT_SCHD_ID", sequenceName = "OFFENDER_EXCLUDE_ACT_SCHD_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_EXCLUDE_ACT_SCHD_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFF_PRGREF_ID", nullable = false)
  var offenderProgramProfile: OffenderProgramProfile,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  var courseActivity: CourseActivity,

  @Column(name = "SLOT_CATEGORY_CODE")
  @Enumerated(EnumType.STRING)
  var slotCategory: SlotCategory? = null,

  @Column(name = "EXCLUDE_DAY")
  @Enumerated(EnumType.STRING)
  var excludeDay: WeekDay,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderActivityExclusion
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String =
    "OffenderActivityExclusion(id = $id , offenderBooking.bookingId = ${offenderBooking.bookingId} , allocation.offenderProgramProfileReferenceId = ${offenderProgramProfile.offenderProgramReferenceId} , courseActivity.courseActivityId = ${courseActivity.courseActivityId} , slotCategory = $slotCategory , excludeDay = $excludeDay)"
}

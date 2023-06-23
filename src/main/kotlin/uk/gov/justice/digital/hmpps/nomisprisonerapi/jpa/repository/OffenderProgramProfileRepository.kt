package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile

@Repository
interface OffenderProgramProfileRepository : JpaRepository<OffenderProgramProfile, Long> {
  fun findByCourseActivityAndOffenderBooking(
    courseActivity: CourseActivity,
    offenderBooking: OffenderBooking,
  ): List<OffenderProgramProfile>

  fun findByCourseActivityCourseActivityIdAndOffenderBookingBookingIdAndProgramStatusCode(
    courseActivityId: Long,
    bookingId: Long,
    code: String,
  ): OffenderProgramProfile?

  fun findByCourseActivity(courseActivity: CourseActivity): List<OffenderProgramProfile>
}

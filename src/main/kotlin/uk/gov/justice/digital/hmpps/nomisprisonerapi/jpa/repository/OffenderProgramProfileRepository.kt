package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile

@Repository
interface OffenderProgramProfileRepository : JpaRepository<OffenderProgramProfile, Long> {
  fun findByCourseActivityAndOffenderBooking(
    courseActivity: CourseActivity,
    offenderBooking: OffenderBooking
  ): OffenderProgramProfile?

  @Query(
    """
    from OffenderProgramProfile opp 
    where opp.courseActivity.courseActivityId = :courseActivityId
     and opp.offenderBooking.bookingId = :bookingId 
     and opp.programStatus = 'ALLOC'"""
  )
  fun findByCourseActivityIdAndOffenderBookingIdAndAlloc(
    courseActivityId: Long,
    bookingId: Long
  ): OffenderProgramProfile?
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.BookingCount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import java.time.LocalDate

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

  fun findByCourseActivityCourseActivityIdAndProgramStatusCode(
    courseActivityId: Long,
    code: String,
  ): List<OffenderProgramProfile>

  fun findByCourseActivity(courseActivity: CourseActivity): List<OffenderProgramProfile>

  @Query(
    value = """
       select opp.offenderProgramReferenceId
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       join CourseActivity ca on opp.courseActivity.courseActivityId = ca.courseActivityId 
       join CourseScheduleRule csr on ca = csr.courseActivity
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate > current_date)
       and ob.active = true
       and ob.location.id = :prisonId
       and ca.prison.id = :prisonId
       and ca.active = true
       and ca.scheduleStartDate <= current_date
       and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date)
       and ca.program.programCode not in :excludeProgramCodes
       and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
       and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
  """,
  )
  fun findActiveAllocations(prisonId: String, excludeProgramCodes: List<String>, courseActivityId: Long?, pageable: Pageable): Page<Long>

  @Suppress("SqlNoDataSourceInspection")
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      update offender_program_profiles opp
      set opp.offender_end_date = :date, opp.offender_end_reason = 'OTH', opp.offender_program_status = 'END'
      where opp.crs_acty_id in :courseActivityIds
      and opp.offender_program_status = 'ALLOC'
      and (opp.offender_end_date is null or opp.offender_end_date > current_date)
    """,
  )
  fun endAllocations(courseActivityIds: Collection<Long>, date: LocalDate)

  @Query(
    value = """
      select new uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.BookingCount(opp.offenderBooking.bookingId, count(opp))
      from OffenderProgramProfile opp 
      join CourseActivity ca on opp.courseActivity = ca
      join OffenderBooking ob on opp.offenderBooking = ob
      where opp.programStatus.code = :prisonerStatusCode
      and opp.startDate <= :date
      and (opp.endDate is null or opp.endDate >= :date)
      and (ca.scheduleEndDate is null or ca.scheduleEndDate >= :date)
      and opp.prison.id = :prisonId
      and ob.location.id = :prisonId
      and opp.suspended = false
      group by opp.offenderBooking.bookingId
      order by opp.offenderBooking.bookingId
    """,
  )
  fun findBookingAllocationCountsByPrisonAndPrisonerStatus(prisonId: String, prisonerStatusCode: String, date: LocalDate = LocalDate.now()): List<BookingCount>
}

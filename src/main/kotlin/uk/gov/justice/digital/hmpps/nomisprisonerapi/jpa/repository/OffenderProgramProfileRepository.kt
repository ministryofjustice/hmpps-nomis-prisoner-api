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

  fun findByCourseActivityCourseActivityIdAndOffenderBookingBookingIdAndProgramStatusCodeAndStartDate(
    courseActivityId: Long,
    bookingId: Long,
    code: String,
    startDate: LocalDate,
  ): OffenderProgramProfile?

  fun findByCourseActivityCourseActivityIdAndProgramStatusCode(
    courseActivityId: Long,
    code: String,
  ): List<OffenderProgramProfile>

  @Query(
    value = """
       select opp.offenderProgramReferenceId
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       join CourseActivity ca on opp.courseActivity.courseActivityId = ca.courseActivityId 
       join CourseScheduleRule csr on ca = csr.courseActivity
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate >= :activeOnDate)
       and ob.active = true
       and ob.location.id = :prisonId
       and ca.prison.id = :prisonId
       and ca.active = true
       and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date)
       and ca.auditModuleName != 'DPS_SYNCHRONISATION'
       and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
       and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
  """,
  )
  fun findActiveAllocations(prisonId: String, courseActivityId: Long?, activeOnDate: LocalDate, pageable: Pageable): Page<Long>

  @Query(
    value = """
       select 
         o.nomsId as nomsId, 
         ca.courseActivityId as courseActivityId, 
         ca.description as courseActivityDescription
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       join Offender o on ob.offender = o
       join CourseActivity ca on opp.courseActivity.courseActivityId = ca.courseActivityId 
       join CourseScheduleRule csr on ca = csr.courseActivity
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate >= :activeOnDate)
       and opp.suspended = true
       and ob.active = true
       and ob.location.id = :prisonId
       and ca.prison.id = :prisonId
       and ca.active = true
       and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date)
       and ca.auditModuleName != 'DPS_SYNCHRONISATION'
       and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
       and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
  """,
  )
  fun findSuspendedAllocations(prisonId: String, courseActivityId: Long?, activeOnDate: LocalDate): List<SuspendedAllocations>

  @Query(
    value = """
       select 
         o.nomsId as nomsId,
         i.iepLevel.code as incentiveLevelCode,
         ca.courseActivityId as courseActivityId, 
         ca.description as courseActivityDescription
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       join Offender o on ob.offender = o
       join CourseActivity ca on opp.courseActivity = ca
       join CourseScheduleRule csr on ca = csr.courseActivity
       join Incentive i on ob = i.id.offenderBooking
       join OffenderProgramProfilePayBand oppb on opp = oppb.id.offenderProgramProfile
       left join CourseActivityPayRate capr on ca = capr.id.courseActivity and capr.payBand.code = oppb.payBand.code and capr.iepLevel.code = i.iepLevel.code and (capr.endDate is null or capr.endDate > current_date)
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate >= :activeOnDate)
       and ob.active = true
       and ob.location.id = :prisonId
       and ca.prison.id = :prisonId
       and ca.active = true
       and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date)
       and ca.auditModuleName != 'DPS_SYNCHRONISATION'
       and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
       and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
       and (oppb.endDate is null or oppb.endDate > current_date)
       and i.id.sequence = (select max(i2.id.sequence) from Incentive i2 where i2.id.offenderBooking = ob)
       and capr.payBand.code is null
       and 0 < (select count(*) from CourseActivityPayRate capr2 where capr2.id.courseActivity = ca)
  """,
  )
  fun findAllocationsMissingPayBands(prisonId: String, courseActivityId: Long?, activeOnDate: LocalDate): List<AllocationsMissingPayRates>

  @Suppress("SqlNoDataSourceInspection")
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      update offender_program_profiles opp
      set opp.offender_end_date = :date, opp.offender_end_reason = 'OTH', opp.offender_program_status = 'END'
      where opp.crs_acty_id in :courseActivityIds
      and opp.offender_program_status = 'ALLOC'
      and (opp.offender_end_date is null or opp.offender_end_date >= current_date)
    """,
  )
  fun endAllocations(courseActivityIds: Collection<Long>, date: LocalDate)

  @Suppress("SqlNoDataSourceInspection")
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      update offender_program_profiles opp
      set opp.offender_end_date = :newEndDate
      where opp.crs_acty_id in :courseActivityIds
      and opp.offender_end_date = :oldEndDate
    """,
  )
  fun moveAllocationEndDate(courseActivityIds: Collection<Long>, oldEndDate: LocalDate, newEndDate: LocalDate)

  // Note that if we're looking for suspensions we don't check bookings in other prisons because we've found that the other
  // prisons often end activities in the old prison so these would give false positives in the reconciliation.
  // Note also that we have started including non-suspended allocations where the prisoner has moved to another prisoner
  // because if for some reason that allocation is not ended then it appears on the DPS prisoner schedule which has been
  // confusing DPS users, so we want to catch these.
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
      and (ob.location.id = :prisonId or :suspended = false)
      and opp.suspended = :suspended
      group by opp.offenderBooking.bookingId
      order by opp.offenderBooking.bookingId
    """,
  )
  fun findBookingAllocationCountsByPrisonAndPrisonerStatus(prisonId: String, prisonerStatusCode: String, date: LocalDate = LocalDate.now(), suspended: Boolean): List<BookingCount>
}

interface SuspendedAllocations {
  fun getNomsId(): String
  fun getCourseActivityId(): Long
  fun getCourseActivityDescription(): String
}

interface AllocationsMissingPayRates {
  fun getNomsId(): String
  fun getIncentiveLevelCode(): String
  fun getCourseActivityId(): Long
  fun getCourseActivityDescription(): String
}

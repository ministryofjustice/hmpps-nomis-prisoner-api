package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import java.time.LocalDate

@Repository
interface CourseActivityRepository : JpaRepository<CourseActivity, Long> {

  @Query(
    value = """
    select ca.courseActivityId from CourseActivity ca 
    join CourseScheduleRule csr on ca = csr.courseActivity
    where ca.prison.id = :prisonId
    and ca.active = true
    and ca.scheduleStartDate <= current_date 
    and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date) 
    and ca.program.programCode not in :excludeProgramCodes
    and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
    and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
    and ca.courseActivityId in
      (
       select distinct(opp.courseActivity.courseActivityId) 
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate > current_date)
       and ob.active = true
       and ob.location.id = :prisonId
      )   
  """,
  )
  fun findActiveActivities(prisonId: String, excludeProgramCodes: List<String>, courseActivityId: Long?, pageRequest: Pageable): Page<Long>

  @Query(
    value = """
    select 
      ca.courseActivityId as courseActivityId, 
      ca.description as courseActivityDescription, 
      capr.payBand.code as payBandCode, 
      capr.id.iepLevelCode as incentiveLevelCode
    from CourseActivity ca 
    join CourseScheduleRule csr on ca = csr.courseActivity
    join CourseActivityPayRate capr on ca = capr.id.courseActivity
    left join PrisonIepLevel pil on :prisonId = pil.agencyLocation.id and capr.id.iepLevelCode = pil.iepLevelCode
      and pil.active = true and (pil.expiryDate is null or pil.expiryDate > current_date)
    where ca.prison.id = :prisonId
    and ca.active = true
    and ca.scheduleStartDate <= current_date 
    and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date) 
    and ca.program.programCode not in :excludeProgramCodes
    and (:courseActivityId is null or ca.courseActivityId = :courseActivityId)
    and csr.id = (select max(id) from CourseScheduleRule where courseActivity = ca)
    and ca.courseActivityId in
      (
       select distinct(opp.courseActivity.courseActivityId) 
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and (opp.endDate is null or opp.endDate > current_date)
       and ob.active = true
       and ob.location.id = :prisonId
      )   
    and (capr.endDate is null or capr.endDate > current_date)
    and pil.iepLevelCode is null
  """,
  )
  fun findPayRatesWithUnknownIncentive(prisonId: String, excludeProgramCodes: List<String>, courseActivityId: Long?): List<PayRateWithUnknownIncentive>

  @Modifying
  @Query(
    value = """
    update CourseActivity ca set ca.scheduleEndDate = :date 
    where ca.courseActivityId in :courseActivityIds
    and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date)
  """,
  )
  fun endActivities(courseActivityIds: Collection<Long>, date: LocalDate)
}

interface PayRateWithUnknownIncentive {
  fun getCourseActivityId(): Long
  fun getCourseActivityDescription(): String
  fun getPayBandCode(): String
  fun getIncentiveLevelCode(): String
}

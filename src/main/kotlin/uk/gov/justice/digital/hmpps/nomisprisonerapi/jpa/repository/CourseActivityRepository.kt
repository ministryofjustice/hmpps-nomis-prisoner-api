package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity

@Repository
interface CourseActivityRepository : JpaRepository<CourseActivity, Long> {

  @Query(
    value = """
    select ca.courseActivityId from CourseActivity ca 
    where ca.prison.id = :prisonId
    and ca.active = true
    and ca.scheduleStartDate <= current_date 
    and (ca.scheduleEndDate is null or ca.scheduleEndDate > current_date) 
    and ca.courseActivityId in
      (
       select distinct(opp.courseActivity.courseActivityId) 
       from OffenderProgramProfile opp
       join OffenderBooking ob on opp.offenderBooking = ob
       where opp.prison.id = :prisonId 
       and opp.programStatus.code = 'ALLOC'
       and opp.startDate <= current_date 
       and (opp.endDate is null or opp.endDate > current_date)
       and ob.active = true
       and ob.location.id = :prisonId
      )   
  """,
  )
  fun findActiveActivities(prisonId: String, pageRequest: Pageable): Page<Long>
}

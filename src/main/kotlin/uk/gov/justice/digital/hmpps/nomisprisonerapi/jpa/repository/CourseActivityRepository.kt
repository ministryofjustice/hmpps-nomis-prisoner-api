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
    where ca.prison.id = :prisonId and ca.courseActivityId in 
      (
       select distinct(opp.courseActivity.courseActivityId) from OffenderProgramProfile opp
       where opp.prison.id = :prisonId and opp.programStatus.code = 'ALLOC'
      )   
  """,
  )
  fun findActivitiesToMigrate(prisonId: String, pageRequest: Pageable): Page<Long>
}

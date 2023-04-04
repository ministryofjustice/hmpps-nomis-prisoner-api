package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import java.time.LocalDate

@Repository
interface CourseScheduleRepository : JpaRepository<CourseSchedule, Long> {
  fun findByCourseActivityAndScheduleDate(courseActivity: CourseActivity, scheduleDate: LocalDate): List<CourseSchedule>
}

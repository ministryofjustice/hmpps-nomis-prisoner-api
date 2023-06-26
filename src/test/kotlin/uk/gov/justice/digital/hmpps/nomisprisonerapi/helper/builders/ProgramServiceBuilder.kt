package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService

class ProgramServiceBuilder(
  val repository: Repository? = null,
  var programCode: String = "INTTEST",
  var programId: Long = 20,
  var description: String = "test program",
  var active: Boolean = true,
) : ProgramServiceDsl {
  fun build() = ProgramService(
    programCode = programCode,
    programId = programId,
    description = description,
    active = active,
  )

  @CourseActivityDslMarker
  override fun courseActivity(
    courseActivityId: Long,
    code: String,
    programId: Long,
    prisonId: String,
    description: String,
    capacity: Int,
    active: Boolean,
    startDate: String,
    endDate: String?,
    minimumIncentiveLevelCode: String,
    internalLocationId: Long?,
    payRates: List<CourseActivityPayRateBuilder>,
    courseSchedules: List<CourseScheduleBuilder>,
    courseScheduleRules: List<CourseScheduleRuleBuilder>,
    excludeBankHolidays: Boolean,
    dsl: CourseActivityDsl.() -> Unit,
  ): CourseActivity =
    repository?.save(
      CourseActivityBuilder(
        repository,
        courseActivityId,
        code,
        programId,
        prisonId,
        description,
        capacity,
        active,
        startDate,
        endDate,
        minimumIncentiveLevelCode,
        internalLocationId,
        payRates,
        courseSchedules,
        courseScheduleRules,
        excludeBankHolidays,
      ).apply(dsl),
      repository.save(this),
    )
      ?: throw IllegalStateException("No repository - is this a unit test? Try create() instead.")
}

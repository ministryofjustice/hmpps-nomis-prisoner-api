package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository

@DslMarker
annotation class ProgramServiceDslMarker

@NomisDataDslMarker
interface ProgramServiceDsl {
  @CourseActivityDslMarker
  fun courseActivity(
    courseActivityId: Long = 0,
    code: String = "CA",
    prisonId: String = "BXI",
    description: String = "test course activity",
    capacity: Int = 23,
    active: Boolean = true,
    startDate: String = "2022-10-31",
    endDate: String? = null,
    internalLocationId: Long? = -3005,
    excludeBankHolidays: Boolean = false,
    payPerSession: String = "H",
    outsideWork: Boolean = false,
    dsl: CourseActivityDsl.() -> Unit = {
      courseSchedule()
      courseScheduleRule()
      payRate()
    },
  ): CourseActivity
}

@Component
class ProgramServiceBuilderRepository(private val programServiceRepository: ProgramServiceRepository) {
  fun save(programService: ProgramService) =
    programServiceRepository.findByProgramCode(programService.programCode)
      ?: programServiceRepository.save(programService)
}

@Component
class ProgramServiceBuilderFactory(
  private val repository: ProgramServiceBuilderRepository? = null,
  private val courseActivityBuilderFactory: CourseActivityBuilderFactory = CourseActivityBuilderFactory(),
) {
  fun builder() = ProgramServiceBuilder(repository, courseActivityBuilderFactory)
}

class ProgramServiceBuilder(
  val repository: ProgramServiceBuilderRepository? = null,
  val courseActivityBuilderFactory: CourseActivityBuilderFactory,
) : ProgramServiceDsl {

  private lateinit var programService: ProgramService

  fun build(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
  ): ProgramService = ProgramService(
    programCode = programCode,
    programId = programId,
    description = description,
    active = active,
  )
    .let { save(it) }
    .also { programService = it }

  @CourseActivityDslMarker
  override fun courseActivity(
    courseActivityId: Long,
    code: String,
    prisonId: String,
    description: String,
    capacity: Int,
    active: Boolean,
    startDate: String,
    endDate: String?,
    internalLocationId: Long?,
    excludeBankHolidays: Boolean,
    payPerSession: String,
    outsideWork: Boolean,
    dsl: CourseActivityDsl.() -> Unit,
  ): CourseActivity = courseActivityBuilderFactory.builder().let { builder ->
    builder.build(
      programService,
      courseActivityId,
      code,
      prisonId,
      description,
      capacity,
      active,
      startDate,
      endDate,
      internalLocationId,
      excludeBankHolidays,
      PayPerSession.valueOf(payPerSession),
      outsideWork,
    )
      .also { programService.courseActivities += it }
      .also { builder.apply(dsl) }
  }

  private fun save(programService: ProgramService) = repository?.save(programService) ?: programService
}

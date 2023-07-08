package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository

@DslMarker
annotation class ProgramServiceDslMarker

@NomisDataDslMarker
interface ProgramServiceDsl : CourseActivityDslApi

interface ProgramServiceDslApi {
  @ProgramServiceDslMarker
  fun programService(
    programCode: String = "INTTEST",
    programId: Long = 20,
    description: String = "test program",
    active: Boolean = true,
    dsl: ProgramServiceDsl.() -> Unit = {},
  ): ProgramService
}

@Component
class ProgramServiceBuilderRepository(private val programServiceRepository: ProgramServiceRepository) {
  fun save(programService: ProgramService) = programServiceRepository.save(programService)
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
    programId: Long,
    prisonId: String,
    description: String,
    capacity: Int,
    active: Boolean,
    startDate: String,
    endDate: String?,
    minimumIncentiveLevelCode: String,
    internalLocationId: Long?,
    excludeBankHolidays: Boolean,
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
      minimumIncentiveLevelCode,
      internalLocationId,
      excludeBankHolidays,
    ).also {
      builder.apply(dsl)
    }
  }

  private fun save(programService: ProgramService) = repository?.save(programService) ?: programService
}

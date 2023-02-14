package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import java.time.LocalDate

@Component
class CourseActivityBuilderFactory(
  private val courseActivityPayRateBuilderFactory: CourseActivityPayRateBuilderFactory = CourseActivityPayRateBuilderFactory(),
  private val repository: Repository? = null,
) {
  fun builder(
    code: String = "CA",
    programId: Long = 20,
    prisonId: String = "LEI",
    description: String = "test course activity",
    capacity: Int = 23,
    active: Boolean = true,
    startDate: String = "2022-10-31",
    endDate: String? = null,
    minimumIncentiveLevelCode: String = "STD",
    internalLocationId: Long? = -8,
    payRates: List<CourseActivityPayRateBuilder> = listOf(courseActivityPayRateBuilderFactory.builder()),
    courseSchedules: List<CourseScheduleBuilder> = listOf(CourseScheduleBuilder()),
    courseScheduleRules: List<CourseScheduleRuleBuilder> = listOf(CourseScheduleRuleBuilder()),
  ): CourseActivityBuilder {
    return CourseActivityBuilder(
      repository,
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
    )
  }
}

class CourseActivityBuilder(
  val repository: Repository?,
  var code: String,
  var programId: Long,
  var prisonId: String,
  var description: String,
  var capacity: Int,
  var active: Boolean,
  var startDate: String,
  var endDate: String?,
  var minimumIncentiveLevelCode: String,
  var internalLocationId: Long?,
  var payRates: List<CourseActivityPayRateBuilder>,
  var courseSchedules: List<CourseScheduleBuilder>,
  var courseScheduleRules: List<CourseScheduleRuleBuilder>,
) {
  fun build(): CourseActivity =
    repository?.let {
      CourseActivity(
        code = code,
        program = repository.lookupProgramService(programId),
        caseloadId = prisonId,
        prison = repository.lookupAgency(prisonId),
        description = description,
        capacity = capacity,
        active = active,
        scheduleStartDate = LocalDate.parse(startDate),
        scheduleEndDate = endDate?.let { LocalDate.parse(it) },
        iepLevel = repository.lookupIepLevel(minimumIncentiveLevelCode),
        internalLocation = internalLocationId?.let { repository.lookupAgencyInternalLocation(it) },
      ).apply {
        payRates.addAll(this@CourseActivityBuilder.payRates.map { it.build(this) })
        courseSchedules.addAll(this@CourseActivityBuilder.courseSchedules.map { it.build(this) })
        courseScheduleRules.addAll(this@CourseActivityBuilder.courseScheduleRules.map { it.build(this, this.scheduleStartDate!!) })
      }
    }
      ?: throw IllegalStateException("No repository - is this a unit test? Try create() instead.")

  fun create(
    programCode: String = "CA",
    internalLocationCode: String? = "CRM1"
  ): CourseActivity =
    CourseActivity(
      code = code,
      program = programService(programCode),
      caseloadId = prisonId,
      prison = prison(prisonId),
      description = description,
      capacity = capacity,
      active = active,
      scheduleStartDate = LocalDate.parse(startDate),
      scheduleEndDate = endDate?.let { LocalDate.parse(it) },
      iepLevel = iepLevel(minimumIncentiveLevelCode),
      internalLocation = internalLocationCode?.let { internalLocation(it) },
    ).apply {
      payRates.addAll(this@CourseActivityBuilder.payRates.map { it.create(this) })
      courseSchedules.addAll(this@CourseActivityBuilder.courseSchedules.map { it.build(this) })
      courseScheduleRules.addAll(this@CourseActivityBuilder.courseScheduleRules.map { it.build(this, this.scheduleStartDate!!) })
    }

  private fun programService(code: String) = ProgramServiceBuilder(programCode = code).build()

  private fun prison(id: String) = AgencyLocation(id = id, description = id, type = AgencyLocationType.PRISON_TYPE, active = true)

  private fun iepLevel(code: String) = IEPLevel(code = code, description = code)

  private fun internalLocation(code: String) = AgencyInternalLocation(
    locationId = internalLocationId!!,
    active = true,
    locationType = "CLAS",
    agencyId = prisonId,
    description = "Classroom 1",
    locationCode = code
  )
}

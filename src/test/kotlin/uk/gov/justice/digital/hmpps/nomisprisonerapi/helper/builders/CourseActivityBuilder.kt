package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate

@Component
class CourseActivityBuilderFactory(
  private val repository: Repository? = null,
) {
  fun builder(
    courseActivityId: Long = 0,
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
    payRates: List<CourseActivityPayRateBuilder> = listOf(),
    courseSchedules: List<CourseScheduleBuilder> = listOf(),
    courseScheduleRules: List<CourseScheduleRuleBuilder> = listOf(),
    courseAllocations: List<OffenderProgramProfileBuilder> = listOf(),
    excludeBankHolidays: Boolean = false,
  ): CourseActivityBuilder {
    return CourseActivityBuilder(
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
    )
  }
}

class CourseActivityBuilder(
  val repository: Repository?,
  val courseActivityId: Long,
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
  var excludeBankHolidays: Boolean,
) : CourseActivityDsl {
  fun build(programService: ProgramService): CourseActivity =
    repository?.let {
      CourseActivity(
        code = code,
        program = programService,
        caseloadId = prisonId,
        prison = repository.lookupAgency(prisonId),
        description = description,
        capacity = capacity,
        active = active,
        scheduleStartDate = LocalDate.parse(startDate),
        scheduleEndDate = endDate?.let { LocalDate.parse(it) },
        iepLevel = repository.lookupIepLevel(minimumIncentiveLevelCode),
        internalLocation = internalLocationId?.let { repository.lookupAgencyInternalLocation(it) },
        excludeBankHolidays = excludeBankHolidays,
      ).apply {
        payRates.addAll(this@CourseActivityBuilder.payRates.map { it.build(this) })
        courseSchedules.addAll(this@CourseActivityBuilder.courseSchedules.map { it.build(this) })
        courseScheduleRules.addAll(this@CourseActivityBuilder.courseScheduleRules.map { it.build(this) })
      }
    }
      ?: throw IllegalStateException("No repository - is this a unit test? Try create() instead.")

  fun create(
    programCode: String = "CA",
    internalLocationCode: String? = "CRM1",
  ): CourseActivity =
    CourseActivity(
      courseActivityId = courseActivityId,
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
      courseScheduleRules.addAll(this@CourseActivityBuilder.courseScheduleRules.map { it.build(this) })
    }

  override fun payRate(
    iepLevelCode: String,
    payBandCode: String,
    startDate: String,
    endDate: String?,
    halfDayRate: Double,
  ) {
    payRates += CourseActivityPayRateBuilderFactory(repository)
      .builder(
        iepLevelCode,
        payBandCode,
        startDate,
        endDate,
        halfDayRate,
      )
  }

  override fun courseSchedule(
    courseScheduleId: Long,
    scheduleDate: String,
    startTime: String,
    endTime: String,
    slotCategory: SlotCategory,
    scheduleStatus: String,
  ) {
    courseSchedules += CourseScheduleBuilder(
      courseScheduleId,
      scheduleDate,
      startTime,
      endTime,
      slotCategory,
      scheduleStatus,
    )
  }

  override fun courseScheduleRule(
    id: Long,
    startTimeHours: Int,
    startTimeMinutes: Int,
    endTimeHours: Int,
    endTimeMinutes: Int,
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    sunday: Boolean,
  ) {
    courseScheduleRules += CourseScheduleRuleBuilder(
      id,
      startTimeHours,
      startTimeMinutes,
      endTimeHours,
      endTimeMinutes,
      monday,
      tuesday,
      wednesday,
      thursday,
      friday,
      saturday,
      sunday,
    )
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
    locationCode = code,
  )
}

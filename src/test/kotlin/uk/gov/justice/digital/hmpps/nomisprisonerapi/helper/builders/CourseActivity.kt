package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseActivityDslMarker

@NomisDataDslMarker
interface CourseActivityDsl : CourseScheduleDslApi, CourseActivityPayRateDslApi, CourseScheduleRuleDslApi

interface CourseActivityDslApi {
  @CourseActivityDslMarker
  fun courseActivity(
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
    excludeBankHolidays: Boolean = false,
    dsl: CourseActivityDsl.() -> Unit = {
      courseSchedule()
      courseScheduleRule()
      payRate()
    },
  ): CourseActivity
}

@Component
class CourseActivityBuilderRepository(
  private val courseActivityRepository: CourseActivityRepository? = null,
  private val agencyLocationRepository: AgencyLocationRepository? = null,
  private val iepLevelRepository: ReferenceCodeRepository<IEPLevel>? = null,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository? = null,
) {
  fun save(courseActivity: CourseActivity) = courseActivityRepository?.save(courseActivity)
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository?.findByIdOrNull(id)!!
  fun lookupIepLevel(code: String): IEPLevel =
    iepLevelRepository?.findByIdOrNull(ReferenceCode.Pk(IEPLevel.IEP_LEVEL, code))!!
  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository?.findByIdOrNull(locationId)
}

@Component
class CourseActivityBuilderFactory(
  private val repository: CourseActivityBuilderRepository? = null,
  private val courseActivityPayRateBuilderFactory: CourseActivityPayRateBuilderFactory = CourseActivityPayRateBuilderFactory(),
  private val courseScheduleBuilderFactory: CourseScheduleBuilderFactory = CourseScheduleBuilderFactory(),
  private val courseScheduleRuleBuilderFactory: CourseScheduleRuleBuilderFactory = CourseScheduleRuleBuilderFactory(),
) {
  fun builder(): CourseActivityBuilder {
    return CourseActivityBuilder(
      repository,
      courseActivityPayRateBuilderFactory,
      courseScheduleBuilderFactory,
      courseScheduleRuleBuilderFactory,
    )
  }
}

class CourseActivityBuilder(
  val repository: CourseActivityBuilderRepository? = null,
  val courseActivityPayRateBuilderFactory: CourseActivityPayRateBuilderFactory,
  val courseScheduleBuilderFactory: CourseScheduleBuilderFactory,
  val courseScheduleRuleBuilderFactory: CourseScheduleRuleBuilderFactory,
) : CourseActivityDsl {

  private lateinit var courseActivity: CourseActivity

  fun build(
    programService: ProgramService,
    courseActivityId: Long,
    code: String,
    prisonId: String,
    description: String,
    capacity: Int,
    active: Boolean,
    startDate: String,
    endDate: String?,
    minimumIncentiveLevelCode: String,
    internalLocationId: Long?,
    excludeBankHolidays: Boolean,
  ): CourseActivity =
    CourseActivity(
      courseActivityId = courseActivityId,
      code = code,
      program = programService,
      caseloadId = prisonId,
      prison = lookupAgency(prisonId),
      description = description,
      capacity = capacity,
      active = active,
      scheduleStartDate = LocalDate.parse(startDate),
      scheduleEndDate = endDate?.let { LocalDate.parse(it) },
      iepLevel = lookupIepLevel(minimumIncentiveLevelCode),
      internalLocation = internalLocationId?.let { lookupAgencyInternalLocation(it, prisonId) },
      excludeBankHolidays = excludeBankHolidays,
    )
      .let { save(it) }
      .also { courseActivity = it }

  override fun payRate(
    iepLevelCode: String,
    payBandCode: String,
    startDate: String,
    endDate: String?,
    halfDayRate: Double,
  ) =
    courseActivityPayRateBuilderFactory.builder().build(
      courseActivity,
      iepLevelCode,
      payBandCode,
      startDate,
      endDate,
      halfDayRate,
    )
      .also { courseActivity.payRates += it }

  override fun courseSchedule(
    courseScheduleId: Long,
    scheduleDate: String,
    startTime: String,
    endTime: String,
    slotCategory: SlotCategory,
    scheduleStatus: String,
  ) = courseScheduleBuilderFactory.builder().build(
    courseActivity,
    courseScheduleId,
    scheduleDate,
    startTime,
    endTime,
    slotCategory,
    scheduleStatus,
  )
    .also {
      courseActivity.courseSchedules += it
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
  ) = courseScheduleRuleBuilderFactory.builder().build(
    courseActivity,
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
    .also {
      courseActivity.courseScheduleRules += it
    }

  private fun save(courseActivity: CourseActivity) = repository?.save(courseActivity)
    ?: courseActivity
  private fun lookupAgency(id: String): AgencyLocation = repository?.lookupAgency(id)
    ?: AgencyLocation(id = id, description = id, type = AgencyLocationType.PRISON_TYPE, active = true)
  private fun lookupIepLevel(code: String): IEPLevel = repository?.lookupIepLevel(code)
    ?: IEPLevel(code = code, description = code)
  private fun lookupAgencyInternalLocation(internalLocationId: Long, prisonId: String) = repository?.lookupAgencyInternalLocation(internalLocationId)
    ?: AgencyInternalLocation(
      locationId = internalLocationId,
      active = true,
      locationType = "CLAS",
      agencyId = prisonId,
      description = "Classroom 1",
      locationCode = internalLocationId.toString(),
    )
}

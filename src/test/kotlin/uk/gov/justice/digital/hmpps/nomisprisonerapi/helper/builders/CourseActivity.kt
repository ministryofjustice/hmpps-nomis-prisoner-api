package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseActivityDslMarker

@NomisDataDslMarker
interface CourseActivityDsl {
  @CourseScheduleDslMarker
  fun courseSchedule(
    courseScheduleId: Long = 0,
    scheduleDate: String = "2022-11-01",
    startTime: String = "08:00",
    endTime: String = "11:00",
    slotCategory: SlotCategory = SlotCategory.AM,
    scheduleStatus: String = "SCH",
  ): CourseSchedule

  @CourseActivityPayRateDslMarker
  fun payRate(
    iepLevelCode: String = "STD",
    payBandCode: String = "5",
    startDate: String = "2022-10-31",
    endDate: String? = null,
    halfDayRate: Double = 3.2,
  ): CourseActivityPayRate

  @CourseScheduleRuleDslMarker
  fun courseScheduleRule(
    id: Long = 0,
    startTimeHours: Int = 9,
    startTimeMinutes: Int = 30,
    endTimeHours: Int = 12,
    endTimeMinutes: Int = 30,
    monday: Boolean = true,
    tuesday: Boolean = true,
    wednesday: Boolean = true,
    thursday: Boolean = true,
    friday: Boolean = true,
    saturday: Boolean = false,
    sunday: Boolean = false,
  ): CourseScheduleRule
}

@Component
class CourseActivityBuilderRepository(
  private val agencyLocationRepository: AgencyLocationRepository? = null,
  private val iepLevelRepository: ReferenceCodeRepository<IEPLevel>? = null,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository? = null,
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType>? = null,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository?.findByIdOrNull(id)!!
  fun lookupIepLevel(code: String): IEPLevel =
    iepLevelRepository?.findByIdOrNull(ReferenceCode.Pk(IEPLevel.IEP_LEVEL, code))!!

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository?.findByIdOrNull(locationId)

  fun lookupInternalLocationType(code: String): InternalLocationType =
    internalLocationTypeRepository?.findByIdOrNull(ReferenceCode.Pk(InternalLocationType.ILOC_TYPE, code))!!
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
    internalLocationId: Long?,
    excludeBankHolidays: Boolean,
    payPerSession: PayPerSession,
    outsideWork: Boolean,
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
      internalLocation = internalLocationId?.let {
        lookupAgencyInternalLocation(
          it,
          lookupLocationType("CLAS"),
          lookupAgency(prisonId),
        )
      },
      excludeBankHolidays = excludeBankHolidays,
      payPerSession = payPerSession,
      outsideWork = outsideWork,
    )
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

  private fun lookupLocationType(code: String) =
    repository?.lookupInternalLocationType(code) ?: InternalLocationType(code, "Classroom")

  private fun lookupAgency(id: String): AgencyLocation = repository?.lookupAgency(id)
    ?: AgencyLocation(id = id, description = id, type = AgencyLocationType.PRISON_TYPE, active = true)

  private fun lookupAgencyInternalLocation(
    internalLocationId: Long,
    locationType: InternalLocationType,
    prison: AgencyLocation,
  ) =
    repository?.lookupAgencyInternalLocation(internalLocationId)
      ?: AgencyInternalLocation(
        locationId = internalLocationId,
        active = true,
        locationType = locationType,
        agency = prison,
        description = "Classroom 1",
        locationCode = internalLocationId.toString(),
      )
}

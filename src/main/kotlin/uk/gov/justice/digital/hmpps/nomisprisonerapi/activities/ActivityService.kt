package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import java.time.LocalDate

@Service
@Transactional
class ActivityService(
  private val activityRepository: ActivityRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val programServiceRepository: ProgramServiceRepository,
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val payRatesService: PayRatesService,
  private val scheduleService: ScheduleService,
  private val scheduleRuleService: ScheduleRuleService,
  private val telemetryClient: TelemetryClient,
) {
  fun createActivity(request: CreateActivityRequest): CreateActivityResponse =
    mapActivityModel(request)
      .apply { payRates.addAll(payRatesService.mapRates(request, this)) }
      .apply { courseSchedules.addAll(scheduleService.mapSchedules(request.schedules, this)) }
      .apply { courseScheduleRules.addAll(scheduleRuleService.mapRules(request, this)) }
      .let { activityRepository.save(it) }
      .also {
        telemetryClient.trackEvent(
          "activity-created",
          mapOf(
            "nomisCourseActivityId" to it.courseActivityId.toString(),
            "prisonId" to it.prison.id,
            "nomisCourseScheduleIds" to it.courseSchedules.map { schedule -> schedule.courseScheduleId }.toString(),
            "nomisCourseActivityPayRateIds" to it.payRates.map { payRate -> payRate.id.toTelemetry() }.toString(),
            "nomisCourseScheduleRuleIds" to it.courseScheduleRules.map { rule -> rule.id }.toString(),
          ),
          null,
        )
      }
      .let { CreateActivityResponse(it.courseActivityId) }

  private fun mapActivityModel(request: CreateActivityRequest): CourseActivity {
    val prison = findPrisonOrThrow(request.prisonId)

    val location = findLocationInPrisonOrThrow(request.internalLocationId, request.prisonId)

    val programService = findProgramServiceOrThrow(request.programCode)

    val prisonIepLevel = findAvailablePrisonIepLevelOrThrow(request.minimumIncentiveLevelCode, prison)

    return CourseActivity(
      code = request.code,
      program = programService,
      caseloadId = request.prisonId,
      prison = prison,
      description = request.description,
      capacity = request.capacity,
      active = true,
      scheduleStartDate = request.startDate,
      scheduleEndDate = request.endDate,
      iepLevel = prisonIepLevel.iepLevel,
      internalLocation = location,
      payPerSession = request.payPerSession,
      excludeBankHolidays = request.excludeBankHolidays,
    )
  }

  fun updateActivity(courseActivityId: Long, request: UpdateActivityRequest) {
    val existingActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id $courseActivityId not found")

    val oldRules = existingActivity.courseScheduleRules.map { it.copy() }
    val oldPayRates = existingActivity.payRates.map { it.copy() }
    val oldSchedules = existingActivity.courseSchedules.map { it.copy() }

    mapActivityModel(existingActivity, request)
      .also { activityRepository.saveAndFlush(it) }
      .also { savedCourseActivity ->
        telemetryClient.trackEvent(
          "activity-updated",
          mapOf(
            "nomisCourseActivityId" to savedCourseActivity.courseActivityId.toString(),
            "prisonId" to savedCourseActivity.prison.id,
          ) +
            scheduleRuleService.buildUpdateTelemetry(oldRules, savedCourseActivity.courseScheduleRules) +
            payRatesService.buildUpdateTelemetry(oldPayRates, savedCourseActivity.payRates) +
            scheduleService.buildUpdateTelemetry(oldSchedules, savedCourseActivity.courseSchedules),
          null,
        )
      }
  }

  private fun mapActivityModel(existingActivity: CourseActivity, request: UpdateActivityRequest): CourseActivity {
    val location = findLocationInPrisonOrThrow(request.internalLocationId, existingActivity.prison.id)

    val prisonIepLevel = findAvailablePrisonIepLevelOrThrow(request.minimumIncentiveLevelCode, existingActivity.prison)

    val requestedProgramService = findProgramServiceOrThrow(request.programCode)

    checkDatesInOrder(request.startDate, request.endDate)

    with(existingActivity) {
      scheduleStartDate = request.startDate
      scheduleEndDate = request.endDate
      internalLocation = location
      capacity = request.capacity
      description = request.description
      iepLevel = prisonIepLevel.iepLevel
      payPerSession = request.payPerSession
      excludeBankHolidays = request.excludeBankHolidays
      payRatesService.buildNewPayRates(request.payRates, this).also { newPayRates ->
        payRates.clear()
        payRates.addAll(newPayRates)
      }
      scheduleRuleService.buildNewRules(request.scheduleRules, this).also { newRules ->
        courseScheduleRules.clear()
        courseScheduleRules.addAll(newRules)
      }
      scheduleService.buildNewSchedules(request.schedules, this).also { newSchedules ->
        courseSchedules.clear()
        courseSchedules.addAll(newSchedules)
      }
      if (program.programCode != requestedProgramService.programCode) {
        program = requestedProgramService
        offenderProgramProfiles
          .filter { it.endDate == null }
          .forEach { it.program = requestedProgramService }
      }
    }

    return existingActivity
  }

  private fun checkDatesInOrder(startDate: LocalDate, endDate: LocalDate?) =
    endDate?.run {
      if (startDate > endDate) {
        throw BadDataException("Start date $startDate must not be after end date $endDate")
      }
    }

  private fun findAvailablePrisonIepLevelOrThrow(iepLevelCode: String, prison: AgencyLocation) =
    availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(prison, iepLevelCode)
      ?: throw BadDataException("IEP type $iepLevelCode does not exist for prison ${prison.id}")

  private fun findProgramServiceOrThrow(programCode: String) =
    (
      programServiceRepository.findByProgramCode(programCode)
        ?: throw BadDataException("Program Service with code=$programCode does not exist")
      )

  private fun findLocationInPrisonOrThrow(internalLocationId: Long?, prisonId: String): AgencyInternalLocation? {
    val location = internalLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Location with id=$it does not exist")
    }?.also {
      if (it.agencyId != prisonId) {
        throw BadDataException("Location with id=$internalLocationId not found in prison $prisonId")
      }
    }
    return location
  }

  private fun findPrisonOrThrow(prisonId: String) =
    (
      agencyLocationRepository.findByIdOrNull(prisonId)
        ?: throw BadDataException("Prison with id=$prisonId does not exist")
      )

  fun deleteActivity(courseActivityId: Long) = activityRepository.deleteById(courseActivityId)
}

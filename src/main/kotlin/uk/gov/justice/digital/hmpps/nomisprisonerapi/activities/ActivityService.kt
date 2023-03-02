package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository

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
  fun createActivity(dto: CreateActivityRequest): CreateActivityResponse =
    mapActivityModel(dto)
      .apply { payRates.addAll(payRatesService.mapRates(dto, this)) }
      .apply { courseSchedules.addAll(scheduleService.mapSchedules(dto.schedules, this)) }
      .apply { courseScheduleRules.addAll(scheduleRuleService.mapRules(dto, this)) }
      .let { activityRepository.save(it) }
      .also {
        telemetryClient.trackEvent(
          "activity-created",
          mapOf(
            "courseActivityId" to it.courseActivityId.toString(),
            "prisonId" to it.prison.id,
            "courseScheduleIds" to it.courseSchedules.map { schedule -> schedule.courseScheduleId }.toString(),
            "courseActivityPayRateIds" to it.payRates.map { payRate -> "${payRate.id.iepLevelCode}-${payRate.id.payBandCode}-${payRate.id.startDate}" }.toString(),
            "courseScheduleRuleIds" to it.courseScheduleRules.map { rule -> rule.id }.toString(),
          ),
          null,
        )
      }
      .let { CreateActivityResponse(it.courseActivityId) }

  private fun mapActivityModel(dto: CreateActivityRequest): CourseActivity {
    val prison = agencyLocationRepository.findByIdOrNull(dto.prisonId)
      ?: throw BadDataException("Prison with id=${dto.prisonId} does not exist")

    val location = dto.internalLocationId?.run {
      agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
        ?: throw BadDataException("Location with id=${dto.internalLocationId} does not exist")
    }

    val programService = programServiceRepository.findByProgramCode(dto.programCode)
      ?: throw BadDataException("Program Service with code=${dto.programCode} does not exist")

    val availablePrisonIepLevel = dto.minimumIncentiveLevelCode?.run {
      availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(prison, dto.minimumIncentiveLevelCode)
        ?: throw BadDataException("IEP type ${dto.minimumIncentiveLevelCode} does not exist for prison ${dto.prisonId}")
    }
    return CourseActivity(
      code = dto.code,
      program = programService,
      caseloadId = dto.prisonId,
      prison = prison,
      description = dto.description,
      capacity = dto.capacity,
      active = true,
      scheduleStartDate = dto.startDate,
      scheduleEndDate = dto.endDate,
      iepLevel = availablePrisonIepLevel!!.iepLevel,
      internalLocation = location,
      payPerSession = dto.payPerSession,
    )
  }

  fun updateActivity(courseActivityId: Long, updateActivityRequest: UpdateActivityRequest) {
    val existingActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id $courseActivityId not found")

    val location = updateActivityRequest.internalLocationId?.run {
      agencyInternalLocationRepository.findByIdOrNull(updateActivityRequest.internalLocationId)
        ?: throw BadDataException("Location with id=${updateActivityRequest.internalLocationId} does not exist")
    }

    location?.also {
      if (location.agencyId != existingActivity.caseloadId) {
        throw BadDataException("Location with id=${updateActivityRequest.internalLocationId} not found in prison ${existingActivity.caseloadId}")
      }
    }

    // TODO SDI-599 Which fields to update and what to do when they are updated will be picked up on this ticket
    existingActivity.scheduleEndDate = updateActivityRequest.endDate
    existingActivity.internalLocation = location
    payRatesService.buildNewPayRates(updateActivityRequest.payRates, existingActivity).also { newPayRates ->
      existingActivity.payRates.clear()
      existingActivity.payRates.addAll(newPayRates)
    }
    scheduleRuleService.buildNewRules(updateActivityRequest.scheduleRules, existingActivity).also { newRules ->
      existingActivity.courseScheduleRules.clear()
      existingActivity.courseScheduleRules.addAll(newRules)
    }
  }

  fun updateActivitySchedules(courseActivityId: Long, scheduleRequests: List<SchedulesRequest>) {
    activityRepository.findByIdOrNull(courseActivityId)
      ?.also { courseActivity ->
        scheduleService.updateSchedules(scheduleRequests, courseActivity)
          .also { updatedSchedules ->
            courseActivity.courseSchedules.clear()
            courseActivity.courseSchedules.addAll(updatedSchedules)
          }
      }
      ?: throw NotFoundException("Activity $courseActivityId not found")
  }

  fun deleteActivity(courseActivityId: Long) = activityRepository.deleteById(courseActivityId)
}

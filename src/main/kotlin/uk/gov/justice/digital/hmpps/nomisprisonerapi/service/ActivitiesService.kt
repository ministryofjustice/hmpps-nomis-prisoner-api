package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository

@Service
@Transactional
class ActivitiesService(
  private val activityRepository: ActivityRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val programServiceRepository: ProgramServiceRepository,
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createActivity(dto: CreateActivityRequest): CreateActivityResponse {

    val courseActivity = activityRepository.save(mapModel(dto))

    val payRates = mapRates(dto, courseActivity)

    courseActivity.payRates = payRates

    telemetryClient.trackEvent(
      "activity-created",
      mapOf(
        "id" to courseActivity.courseActivityId.toString(),
        "prisonId" to courseActivity.prison.id,
      ),
      null
    )
    log.debug("Activity created with Nomis id = (${courseActivity.courseActivityId})")

    return CreateActivityResponse(courseActivity.courseActivityId)
  }

  private fun mapModel(dto: CreateActivityRequest): CourseActivity {

    val prison = agencyLocationRepository.findById(dto.prisonId)
      .orElseThrow(BadDataException("Prison with id=${dto.prisonId} does not exist"))

    val location = agencyInternalLocationRepository.findById(dto.internalLocation)
      .orElseThrow(BadDataException("Location with id=${dto.internalLocation} does not exist"))

    val programService = programServiceRepository.findByProgramCode(dto.programCode)
      ?: throw BadDataException("Program Service with code=${dto.programCode} does not exist")

    val availablePrisonIepLevel = dto.minimumIncentiveLevel?.run {
      availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(prison, dto.minimumIncentiveLevel)
        ?: throw BadDataException("IEP type ${dto.minimumIncentiveLevel} does not exist for prison ${dto.prisonId}")
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
      payPerSession = PayPerSession.F,
    )
  }

  private fun mapRates(dto: CreateActivityRequest, courseActivity: CourseActivity): List<CourseActivityPayRate> {

    return dto.payRates.map { rate ->

      val availablePrisonIepLevel = rate.incentiveLevel?.run {
        return@run availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(courseActivity.prison, rate.incentiveLevel)
          ?: throw BadDataException("IEP type ${rate.incentiveLevel} does not exist for prison ${dto.prisonId}")
      }

      return@map CourseActivityPayRate(
        courseActivity = courseActivity,
        iepLevelCode = availablePrisonIepLevel!!.iepLevel.code,
        payBandCode = rate.payBand,
        startDate = dto.startDate,
        endDate = dto.endDate,
        halfDayRate = rate.rate,
      )
    }
  }
}

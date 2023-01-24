package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository

@Service
@Transactional
class ActivitiesService(
  private val activityRepository: ActivityRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val programServiceRepository: ProgramServiceRepository,
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createActivity(dto: CreateActivityRequest): CreateActivityResponse =

    activityRepository.save(mapActivityModel(dto)).run {

      payRates = mapRates(dto, this)

      telemetryClient.trackEvent(
        "activity-created",
        mapOf(
          "id" to courseActivityId.toString(),
          "prisonId" to prison.id,
        ),
        null
      )

      CreateActivityResponse(courseActivityId)
    }

  fun createOffenderProgramProfile(courseActivityId: Long, dto: CreateOffenderProgramProfileRequest): CreateOffenderProgramProfileResponse =

    offenderProgramProfileRepository.save(mapOffenderProgramProfileModel(courseActivityId, dto)).run {

      telemetryClient.trackEvent(
        "offender-program-profile-created",
        mapOf(
          "id" to offenderProgramReferenceId.toString(),
          "courseActivityId" to courseActivity?.courseActivityId.toString(),
          "bookingId" to offenderBooking.bookingId.toString(),
        ),
        null
      )
      log.debug("OffenderProgramProfile created with Nomis id = $offenderProgramReferenceId")

      CreateOffenderProgramProfileResponse(offenderProgramReferenceId)
    }

  private fun mapActivityModel(dto: CreateActivityRequest): CourseActivity {

    val prison = agencyLocationRepository.findByIdOrNull(dto.prisonId)
      ?: throw BadDataException("Prison with id=${dto.prisonId} does not exist")

    val location = agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
      ?: throw BadDataException("Location with id=${dto.internalLocationId} does not exist")

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
      payPerSession = PayPerSession.H,
    )
  }

  private fun mapRates(dto: CreateActivityRequest, courseActivity: CourseActivity): MutableList<CourseActivityPayRate> {

    return dto.payRates.map { rate ->

      val availablePrisonIepLevel = availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(
        courseActivity.prison,
        rate.incentiveLevel
      )
        ?: throw BadDataException("IEP type ${rate.incentiveLevel} does not exist for prison ${dto.prisonId}")

      return@map CourseActivityPayRate(
        courseActivity = courseActivity,
        iepLevelCode = availablePrisonIepLevel.iepLevel.code,
        payBandCode = rate.payBand,
        startDate = dto.startDate,
        endDate = dto.endDate,
        halfDayRate = rate.rate,
      )
    }.toMutableList()
  }

  private fun mapOffenderProgramProfileModel(courseActivityId: Long, dto: CreateOffenderProgramProfileRequest): OffenderProgramProfile {

    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    val offenderBooking = offenderBookingRepository.findByIdOrNull(dto.bookingId)
      ?: throw BadDataException("Booking with id=${dto.bookingId} does not exist")

    return OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = dto.startDate,
      programStatus = "ALLOC",
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = dto.endDate,
    )
  }

  // TODO SDI-500 make this work
  fun updateActivity(courseActivityId: Long, updateActivityRequest: UpdateActivityRequest): UpdateActivityResponse =
    UpdateActivityResponse(prisonId = updateActivityRequest.prisonId)
}

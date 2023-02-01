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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

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
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createActivity(dto: CreateActivityRequest): CreateActivityResponse =
    mapActivityModel(dto)
      .apply { payRates.addAll(mapRates(dto, this)) }
      .also {
        telemetryClient.trackEvent(
          "activity-created",
          mapOf(
            "id" to it.courseActivityId.toString(),
            "prisonId" to it.prison.id,
          ),
          null
        )
      }
      .let { CreateActivityResponse(activityRepository.save(it).courseActivityId) }

  fun createOffenderProgramProfile(
    courseActivityId: Long,
    dto: CreateOffenderProgramProfileRequest
  ): CreateOffenderProgramProfileResponse =

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

      val payBand = payBandRepository.findByIdOrNull(PayBand.pk(rate.payBand))
        ?: throw BadDataException("Pay band code ${rate.payBand} does not exist")

      return@map CourseActivityPayRate(
        id = CourseActivityPayRateId(
          courseActivity = courseActivity,
          iepLevelCode = availablePrisonIepLevel.iepLevel.code,
          payBandCode = payBand.code,
          startDate = dto.startDate,
        ),
        payBand = payBand,
        endDate = dto.endDate,
        halfDayRate = CourseActivityPayRate.preciseHalfDayRate(rate.rate),
      )
    }.toMutableList()
  }

  private fun mapOffenderProgramProfileModel(
    courseActivityId: Long,
    dto: CreateOffenderProgramProfileRequest
  ): OffenderProgramProfile {

    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    val offenderBooking = offenderBookingRepository.findByIdOrNull(dto.bookingId)
      ?: throw BadDataException("Booking with id=${dto.bookingId} does not exist")

    val payBand = payBandRepository.findByIdOrNull(PayBand.pk(dto.payBandCode))
      ?: throw BadDataException("Pay band code ${dto.payBandCode} does not exist")

    offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseActivity, offenderBooking)?.run {
      throw BadDataException("Offender Program Profile with courseActivityId=$courseActivityId and bookingId=${dto.bookingId} already exists")
    }

    if (courseActivity.prison.id != offenderBooking.location?.id) {
      throw BadDataException("Prisoner is at prison=${offenderBooking.location?.id}, not the Course activity prison=${courseActivity.prison.id}")
    }

    if (courseActivity.scheduleEndDate?.isBefore(LocalDate.now()) == true) {
      throw BadDataException("Course activity with id=$courseActivityId has expired")
    }

    if (courseActivity.payRates.find { it.payBand == payBand } == null) {
      throw BadDataException("Pay band code ${payBand.code} does not exist for course activity with id=$courseActivityId")
    }

    return OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = dto.startDate,
      programStatus = "ALLOC",
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = dto.endDate,
    ).apply {
      payBands.add(
        OffenderProgramProfilePayBand(
          offenderProgramProfile = this,
          startDate = dto.startDate,
          payBand = payBand,
          endDate = dto.endDate,
        )
      )
    }
  }

  fun updateActivity(courseActivityId: Long, updateActivityRequest: UpdateActivityRequest) {
    val existingActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id $courseActivityId not found")

    val location = agencyInternalLocationRepository.findByIdOrNull(updateActivityRequest.internalLocationId)
      ?: throw BadDataException("Location with id=${updateActivityRequest.internalLocationId} does not exist")

    existingActivity.internalLocation = location
    buildNewPayRates(updateActivityRequest.payRates, existingActivity).also { newPayRates ->
      existingActivity.payRates.clear()
      existingActivity.payRates.addAll(newPayRates)
    }
  }

  /*
   * Rebuild the list of pay rates to replace the existing list - taking into account we never delete old rates, just expire them
   *
   * This is quite a tricky algorithm. When building the new rate list it does the following:
   * - newly requested rates are created and become effective today
   * - any existing rates that are still active but haven't changed are retained
   * - any existing rates that have changed are expired with today's date and a new rate added effective tomorrow
   * - any existing rates that are expired are retained
   * - any existing rates that are not included in the new request are expired
   */
  private fun buildNewPayRates(requestedPayRates: List<PayRateRequest>, existingActivity: CourseActivity): MutableList<CourseActivityPayRate> {
    val newPayRates = mutableListOf<CourseActivityPayRate>()
    val existingPayRates = existingActivity.payRates

    requestedPayRates.forEach { requestedPayRate ->
      val existingPayRate = existingPayRates.findExistingPayRate(requestedPayRate)
      when {
        existingPayRate == null -> newPayRates.add(requestedPayRate.toCourseActivityPayRate(existingActivity))
        existingPayRate.rateIsUnchanged(requestedPayRate) -> newPayRates.add(existingPayRate)
        existingPayRate.rateIsChangedButNotYetActive(requestedPayRate) -> newPayRates.add(existingPayRate.apply { halfDayRate = requestedPayRate.rate }) // e.g. rate adjusted twice in same day
        existingPayRate.rateIsChanged(requestedPayRate) -> {
          newPayRates.add(existingPayRate.expire())
          newPayRates.add(requestedPayRate.toCourseActivityPayRate(existingActivity))
        }
      }
    }

    newPayRates.addAll(existingPayRates.getExpiredPayRates())
    newPayRates.addAll(existingPayRates.expirePayRatesIfMissingFrom(newPayRates))

    return newPayRates
  }

  private fun MutableList<CourseActivityPayRate>.findExistingPayRate(requested: PayRateRequest) =
    firstOrNull { existing ->
      !existing.hasExpiryDate() &&
        requested.payBand == existing.id.payBandCode &&
        requested.incentiveLevel == existing.id.iepLevelCode
    }

  private fun CourseActivityPayRate.rateIsUnchanged(requested: PayRateRequest) =
    this.halfDayRate.compareTo(requested.rate) == 0

  private fun CourseActivityPayRate.rateIsChangedButNotYetActive(requested: PayRateRequest) =
    this.rateIsChanged(requested) && this.hasFutureStartDate()

  private fun CourseActivityPayRate.rateIsChanged(requested: PayRateRequest) =
    this.halfDayRate.compareTo(requested.rate) != 0

  private fun MutableList<CourseActivityPayRate>.containsRate(newPayRate: CourseActivityPayRate) =
    this.firstOrNull { existing ->
      !existing.hasExpiryDate() &&
        existing.id.payBandCode == newPayRate.id.payBandCode &&
        existing.id.iepLevelCode == newPayRate.id.iepLevelCode
    } != null

  private fun MutableList<CourseActivityPayRate>.getExpiredPayRates() = this.filter { it.hasExpiryDate() }

  private fun MutableList<CourseActivityPayRate>.expirePayRatesIfMissingFrom(newPayRates: MutableList<CourseActivityPayRate>) =
    this.filter { old -> !old.hasExpiryDate() }
      .filter { old -> !old.hasFutureStartDate() } // ignore future rates not included in updates - so they are deleted
      .filter { old -> !newPayRates.containsRate(old) }
      .map { old -> old.expire() }

  private fun PayRateRequest.toCourseActivityPayRate(courseActivity: CourseActivity): CourseActivityPayRate {
    val payBand = payBandRepository.findByIdOrNull(PayBand.pk(payBand))
      ?: throw BadDataException("Pay band code $payBand does not exist")

    availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(courseActivity.prison, incentiveLevel)
      ?: throw BadDataException("IEP type $incentiveLevel does not exist for prison ${courseActivity.prison.id}")

    // calculate start date - usually today unless the old rate expires at the end of today
    val today = LocalDate.now()
    val startDate = courseActivity.payRates
      .filter { it.id.iepLevelCode == incentiveLevel && it.payBand == payBand }
      .takeIf { it.isNotEmpty() }
      ?.maxBy { it.id.startDate }
      ?.endDate
      ?.let { if (it < today) today else it.plusDays(1) }
      ?: today

    return CourseActivityPayRate(
      id = CourseActivityPayRateId(
        courseActivity,
        incentiveLevel,
        payBand.code,
        startDate,
      ),
      payBand = payBand,
      endDate = null,
      halfDayRate = CourseActivityPayRate.preciseHalfDayRate(rate)
    )
  }
}

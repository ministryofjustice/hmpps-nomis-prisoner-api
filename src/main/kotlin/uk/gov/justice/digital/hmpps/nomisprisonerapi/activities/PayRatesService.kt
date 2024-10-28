package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.PayRatesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class PayRatesService(
  private val availablePrisonIepLevelRepository: PrisonIepLevelRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
) {

  fun mapRates(dto: CreateActivityRequest, courseActivity: CourseActivity): MutableList<CourseActivityPayRate> {
    return dto.payRates.map { rate ->

      val availablePrisonIepLevel = availablePrisonIepLevelRepository.findFirstByAgencyLocationAndIepLevelCodeAndActive(
        courseActivity.prison,
        rate.incentiveLevel,
      )
        ?: throw BadDataException("Pay rate IEP type ${rate.incentiveLevel} does not exist for prison ${dto.prisonId}")

      val payBand = payBandRepository.findByIdOrNull(PayBand.pk(rate.payBand))
        ?: throw BadDataException("Pay band code ${rate.payBand} does not exist")

      return@map CourseActivityPayRate(
        id = CourseActivityPayRateId(
          courseActivity = courseActivity,
          iepLevelCode = availablePrisonIepLevel.iepLevelCode,
          payBandCode = payBand.code,
          startDate = dto.startDate,
        ),
        payBand = payBand,
        iepLevel = availablePrisonIepLevel.iepLevel,
        endDate = dto.endDate,
        halfDayRate = CourseActivityPayRate.preciseHalfDayRate(rate.rate),
      )
    }.toMutableList()
  }

  fun mapRates(payRates: List<CourseActivityPayRate>): List<PayRatesResponse> =
    payRates
      .filter(CourseActivityPayRate::isActive)
      .map {
        PayRatesResponse(
          incentiveLevelCode = it.iepLevel.code,
          payBand = it.payBand.code,
          rate = it.halfDayRate,
        )
      }

  /*
   * Rebuild the list of pay rates to replace the existing list.
   *
   * This is complicated by the following:
   * - DPS doesn't support end dates so we have to derive them from the start dates
   * - DPS allows hard deletes of rates, but we can't delete them from NOMIS
   */
  fun buildNewPayRates(requestedPayRates: List<PayRateRequest>, existingActivity: CourseActivity): List<CourseActivityPayRate> {
    val newRates = mutableListOf<CourseActivityPayRate>()
    val expiredRates = mutableListOf<CourseActivityPayRate>()
    val existingRates = existingActivity.payRates

    // DPS doesn't support end dates so derive them from the start dates / activity dates
    val requestedRates = requestedPayRates.deriveDates(existingActivity.scheduleStartDate, existingActivity.scheduleEndDate)

    // keep any existing rates that have expired - they might no longer exist in DPS but we can't delete them from NOMIS
    expiredRates.addAll(existingRates.filterExpired())

    // if there are active rates in NOMIS which are different to the requested rate, expire them
    expiredRates.addAll(existingRates.expireChangedRates(requestedRates))

    newRates.addAll(expiredRates)

    // map all rates that are active either today or in the future
    newRates.addAll(
      requestedRates.filterNotExpired()
        .mapNotNull { adjustOverlappingRates(it, expiredRates) }
        .map { it.toCourseActivityPayRate(existingActivity, it.startDate!!, it.endDate) },
    )

    // Rates that are missing but still active in NOMIS must have been deleted in DPS - expire them in NOMIS
    newRates.addAll(
      existingRates
        .filter { it.isActive() }
        .filterNotActiveIn(newRates)
        .map { it.expire() }
        .also { it.throwIfPayBandsInUse() },
    )

    return newRates
  }

  /*
   * As there may be rates in NOMIS that DPS doesn't know about we have to adjust dates for them
   */
  private fun adjustOverlappingRates(
    req: PayRateRequest,
    expiredRates: MutableList<CourseActivityPayRate>,
  ): PayRateRequest? {
    val lastExpiryForType = expiredRates.filter { exp -> exp.toRateType() == req.toRateType() }.maxOfOrNull { it.endDate!! }
    return if (req.endDate != null && lastExpiryForType != null && req.endDate <= lastExpiryForType) {
      // The requested rate doesn't make sense when compared with the last expiry on NOMIS - this must be corrupted data
      // so ignore the request in favour of what's already on NOMIS
      null
    } else if (lastExpiryForType != null &&
      req.isActive() &&
      req.startDate!! <= lastExpiryForType
    ) {
      // The requested rate overlaps with the last expiry on NOMIS - adjust the start date to the day after the last expiry
      req.copy(startDate = lastExpiryForType.plusDays(1))
    } else {
      req
    }
  }

  private fun PayRateRequest.isActive() = startDate!! <= LocalDate.now() && (endDate == null || endDate >= LocalDate.now())

  // Find any rates that are not active in the list of new rates
  private fun List<CourseActivityPayRate>.filterNotActiveIn(newRates: List<CourseActivityPayRate>) =
    filter { existing ->
      newRates.none { new ->
        new.isActive() && (new.toRateType() == existing.toRateType())
      }
    }

  private data class RateType(val iepLevel: String, val payBand: String)
  private fun PayRateRequest.toRateType() = RateType(incentiveLevel, payBand)
  private fun CourseActivityPayRate.toRateType() = RateType(id.iepLevelCode, id.payBandCode)

  /*
   * NOMIS needs start and end dates, but DPS only provides start dates (except for on the first rate which has null start date).
   * Map the requested rates to have start and end dates.
   */
  private fun List<PayRateRequest>.deriveDates(courseStartDate: LocalDate, courseEndDate: LocalDate?): List<PayRateRequest> =
    this.groupBy { it.toRateType() }
      .flatMap { (_, requestedRatesForType) ->
        requestedRatesForType
          .sortedBy { it.startDate }
          .windowed(size = 2, step = 1, partialWindows = true)
          .map {
            val requestedRate: PayRateRequest = it[0]
            val nextRequestedRate: PayRateRequest? = if (it.size > 1) it[1] else null

            val startDate =
              // Use the start date as requested by DPS
              requestedRate.startDate
                // If there is no DPS start date, start at the same time as the course
                ?: courseStartDate

            val endDate = if (nextRequestedRate == null) {
              // this is the last rate so end with the course unless requested to end before then
              minDate(requestedRate.endDate, courseEndDate)
            } else {
              // if there is a next rate then the requested rate ends the day before it
              nextRequestedRate.startDate!!.minusDays(1)
            }

            requestedRate.copy(startDate = startDate, endDate = endDate)
          }
      }

  /*
   * Expire any active pay rates where the rate has changed in DPS and return them
   */
  private fun List<CourseActivityPayRate>.expireChangedRates(requestedRates: List<PayRateRequest>): List<CourseActivityPayRate> =
    this.groupBy { it.toRateType() }
      .mapNotNull { (rateType, existingRatesForType) ->
        val requestedRatesForType = requestedRates.filter { it.toRateType() == rateType }
        val existingActiveRate = existingRatesForType.findActiveRate()
        val requestedActiveRate = requestedRatesForType.findActiveRate()

        // The rate has changed - expire the existing rate
        if (existingActiveRate != null &&
          requestedActiveRate != null &&
          existingActiveRate.halfDayRate.compareTo(requestedActiveRate.rate) != 0
        ) {
          existingActiveRate.expire()
        } else {
          null
        }
      }

  private fun List<CourseActivityPayRate>.filterExpired(): List<CourseActivityPayRate> =
    filter { it.endDate != null && it.endDate!! < LocalDate.now() }

  private fun List<PayRateRequest>.filterNotExpired(): List<PayRateRequest> =
    filter { it.endDate == null || it.endDate >= LocalDate.now() }

  private fun List<PayRateRequest>.findActiveRate(): PayRateRequest? =
    find { it.startDate!! <= LocalDate.now() && (it.endDate == null || it.endDate >= LocalDate.now()) }

  private fun List<CourseActivityPayRate>.findActiveRate(): CourseActivityPayRate? =
    find { it.id.startDate <= LocalDate.now() && (it.endDate == null || it.endDate!! >= LocalDate.now()) }

  private fun minDate(date1: LocalDate?, date2: LocalDate?): LocalDate? =
    when {
      date1 == null -> date2
      date2 == null -> date1
      date1 < date2 -> date1
      else -> date2
    }

  /*
   * Fails if any of the pay rates are in use, because we are trying to delete the pay rates
   */
  private fun List<CourseActivityPayRate>.throwIfPayBandsInUse() =
    this.forEach { activityPayRate ->
      offenderProgramProfileRepository.findByCourseActivityCourseActivityIdAndProgramStatusCode(activityPayRate.id.courseActivity.courseActivityId, "ALLOC")
        .filter { profile -> profile.isPayRateApplicable(activityPayRate.payBand.code, activityPayRate.iepLevel.code) }
        .map { offender -> offender.offenderBooking.offender.nomsId }
        .toList()
        .takeIf { nomsIds -> nomsIds.isNotEmpty() }
        ?.run { throw BadDataException("Pay band ${activityPayRate.payBand.code} for incentive level ${activityPayRate.iepLevel.code} is allocated to offender(s) $this") }
    }

  // Map the requested rate to a NOMIS pay rate
  private fun PayRateRequest.toCourseActivityPayRate(courseActivity: CourseActivity, startDate: LocalDate, endDate: LocalDate? = null): CourseActivityPayRate {
    val payBand = payBandRepository.findByIdOrNull(PayBand.pk(payBand))
      ?: throw BadDataException("Pay band code $payBand does not exist")

    val availableIepLevel = availablePrisonIepLevelRepository.findFirstByAgencyLocationAndIepLevelCodeAndActive(courseActivity.prison, incentiveLevel)
      ?: throw BadDataException("Pay rate IEP type $incentiveLevel does not exist for prison ${courseActivity.prison.id}")

    return CourseActivityPayRate(
      id = CourseActivityPayRateId(
        courseActivity,
        incentiveLevel,
        payBand.code,
        startDate,
      ),
      payBand = payBand,
      iepLevel = availableIepLevel.iepLevel,
      endDate = endDate,
      halfDayRate = CourseActivityPayRate.preciseHalfDayRate(rate),
    )
  }

  fun buildUpdateTelemetry(
    savedPayRates: List<CourseActivityPayRate>,
    newPayRates: List<CourseActivityPayRate>,
  ): Map<String, String> {
    val createdIds = findCreatedIds(savedPayRates, newPayRates)
    val updatedIds = findUpdatedIds(savedPayRates, newPayRates)
    val expiredIds = findExpiredIds(savedPayRates, newPayRates)
    val telemetry = mutableMapOf<String, String>()
    if (createdIds.isNotEmpty()) {
      telemetry["created-courseActivityPayRateIds"] = createdIds.map { it.toTelemetry() }.toString()
    }
    if (updatedIds.isNotEmpty()) {
      telemetry["updated-courseActivityPayRateIds"] = updatedIds.map { it.toTelemetry() }.toString()
    }
    if (expiredIds.isNotEmpty()) {
      telemetry["expired-courseActivityPayRateIds"] = expiredIds.map { it.toTelemetry() }.toString()
    }
    return telemetry.toMap()
  }

  private fun findCreatedIds(
    savedRates: List<CourseActivityPayRate>,
    newRates: List<CourseActivityPayRate>,
  ) = newRates.map { it.id } - savedRates.map { it.id }.toSet()

  private fun findExpiredIds(
    savedRates: List<CourseActivityPayRate>,
    newRates: List<CourseActivityPayRate>,
  ) =
    savedRates
      .filter { savedRate -> !savedRate.hasExpiryDate() }
      .filter { activeSavedRate -> newRates.isNowExpired(activeSavedRate) }
      .map { it.id }

  private fun List<CourseActivityPayRate>.isNowExpired(savedRate: CourseActivityPayRate) =
    find { newRate -> newRate.id == savedRate.id && newRate.hasExpiryDate() } != null

  private fun findUpdatedIds(
    savedRates: List<CourseActivityPayRate>,
    newRates: List<CourseActivityPayRate>,
  ) =
    savedRates
      .filter { it.hasFutureStartDate() }
      .filter { savedRateNotYetActive -> newRates.rateHasChanged(savedRateNotYetActive) }
      .map { it.id }

  private fun List<CourseActivityPayRate>.rateHasChanged(savedRate: CourseActivityPayRate) =
    find { it.id == savedRate.id && it.halfDayRate.compareTo(savedRate.halfDayRate) != 0 } != null
}

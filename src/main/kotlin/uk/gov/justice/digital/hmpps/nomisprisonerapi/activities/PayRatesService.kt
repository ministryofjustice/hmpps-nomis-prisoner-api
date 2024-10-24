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
   *
   * To handle the delete problem we expire rates in NOMIS that are no longer in the DPS requested rates.
   */
  fun buildNewPayRates(requestedPayRates: List<PayRateRequest>, existingActivity: CourseActivity): List<CourseActivityPayRate> {
    val newRates = mutableListOf<CourseActivityPayRate>()
    val existingRates = existingActivity.payRates

    // keep any existing rates that have expired - they might have been deleted from DPS but we can't delete them from NOMIS
    val expiredRates = existingRates.filter { it.endDate != null && it.endDate!! < LocalDate.now() }
    newRates.addAll(expiredRates)

    // DPS doesn't support end dates so derive them from the start dates
    val requestedRates = requestedPayRates.deriveDates(existingActivity.scheduleStartDate, existingActivity.scheduleEndDate, expiredRates)

    // map all rates that are active either today or in the future
    newRates.addAll(
      requestedRates.findActiveRequestedRates()
        .map { it.toCourseActivityPayRate(existingActivity, it.startDate!!, it.endDate) },
    )

    // Rates that are missing from the request but are still active must have been deleted in DPS - expire them in NOMIS
    newRates.addAll(
      existingRates.filter { !newRates.containsRate(it) && it.isActive() }
        .map { it.expire() }
        .also { expiredPayRates -> expiredPayRates.throwIfPayBandsInUse() },
    )

    return newRates
  }

  private fun List<PayRateRequest>.deriveDates(courseStartDate: LocalDate, courseEndDate: LocalDate?, expiredRates: List<CourseActivityPayRate>): List<PayRateRequest> {
    data class RateType(val iepLevel: String, val payBand: String)
    fun PayRateRequest.toRateType() = RateType(incentiveLevel, payBand)
    fun CourseActivityPayRate.toRateType() = RateType(id.iepLevelCode, id.payBandCode)

    return this.groupBy { it.toRateType() }
      .flatMap { (rateType, requestedRates) ->
        requestedRates
          .sortedBy { it.startDate }
          .windowed(size = 2, step = 1, partialWindows = true)
          .map {
            val lastExpiredRate = expiredRates.filter { exp -> exp.toRateType() == rateType }.maxByOrNull { exp -> exp.endDate!! }
            val requestedRate: PayRateRequest = it[0]
            val nextRequestedRate: PayRateRequest? = if (it.size > 1) it[1] else null

            val startDate =
              // Use the start date as requested by DPS
              requestedRate.startDate
                // If there is no DPS start date, this must be the first rate in DPS
                ?: if (lastExpiredRate == null || requestedRate.rate.compareTo(lastExpiredRate.halfDayRate) == 0) {
                  // There is no lastExpiredRate or it's the current rate, then align with the course activity start date
                  courseStartDate
                } else {
                  // DPS appears to have deleted the lastExpiredRate, but NOMIS must start after the expired rate
                  lastExpiredRate.endDate!!.plusDays(1)
                }

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
  }

  private fun List<PayRateRequest>.findActiveRequestedRates(): List<PayRateRequest> =
    filter { it.endDate == null || it.endDate >= LocalDate.now() }

  private fun minDate(date1: LocalDate?, date2: LocalDate?): LocalDate? =
    when {
      date1 == null -> date2
      date2 == null -> date1
      date1 < date2 -> date1
      else -> date2
    }

  private fun MutableList<CourseActivityPayRate>.containsRate(newPayRate: CourseActivityPayRate) =
    this.firstOrNull { existing ->
      existing.id.payBandCode == newPayRate.id.payBandCode &&
        existing.id.iepLevelCode == newPayRate.id.iepLevelCode
    } != null

  private fun List<CourseActivityPayRate>.throwIfPayBandsInUse() =
    this.forEach { activityPayRate ->
      offenderProgramProfileRepository.findByCourseActivityCourseActivityIdAndProgramStatusCode(activityPayRate.id.courseActivity.courseActivityId, "ALLOC")
        .filter { profile -> profile.isPayRateApplicable(activityPayRate.payBand.code, activityPayRate.iepLevel.code) }
        .map { offender -> offender.offenderBooking.offender.nomsId }
        .toList()
        .takeIf { nomsIds -> nomsIds.isNotEmpty() }
        ?.run { throw BadDataException("Pay band ${activityPayRate.payBand.code} for incentive level ${activityPayRate.iepLevel.code} is allocated to offender(s) $this") }
    }

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

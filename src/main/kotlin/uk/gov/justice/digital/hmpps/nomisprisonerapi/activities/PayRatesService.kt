package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class PayRatesService(
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
) {

  fun mapRates(dto: CreateActivityRequest, courseActivity: CourseActivity): MutableList<CourseActivityPayRate> {
    return dto.payRates.map { rate ->

      val availablePrisonIepLevel = availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(
        courseActivity.prison,
        rate.incentiveLevel,
      )
        ?: throw BadDataException("Pay rate IEP type ${rate.incentiveLevel} does not exist for prison ${dto.prisonId}")

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
        iepLevel = availablePrisonIepLevel.iepLevel,
        endDate = dto.endDate,
        halfDayRate = CourseActivityPayRate.preciseHalfDayRate(rate.rate),
      )
    }.toMutableList()
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
  fun buildNewPayRates(requestedPayRates: List<PayRateRequest>, existingActivity: CourseActivity): MutableList<CourseActivityPayRate> {
    val newPayRates = mutableListOf<CourseActivityPayRate>()
    val existingPayRates = existingActivity.payRates
    val tomorrow = LocalDate.now().plusDays(1)

    requestedPayRates.forEach { requestedPayRate ->
      val existingPayRate = existingPayRates.findExistingPayRate(requestedPayRate)
      when {
        existingPayRate == null -> newPayRates.add(requestedPayRate.toCourseActivityPayRate(existingActivity, getStartDate(requestedPayRate.incentiveLevel, requestedPayRate.payBand, existingPayRates)))
        existingPayRate.rateIsUnchanged(requestedPayRate) -> newPayRates.add(existingPayRate)
        existingPayRate.rateIsChangedButNotYetActive(requestedPayRate) -> newPayRates.add(existingPayRate.apply { halfDayRate = requestedPayRate.rate }) // e.g. rate adjusted twice in same day
        existingPayRate.rateIsChanged(requestedPayRate) -> {
          newPayRates.add(existingPayRate.expire())
          newPayRates.add(requestedPayRate.toCourseActivityPayRate(existingActivity, tomorrow))
        }
      }
    }

    newPayRates.addAll(existingPayRates.getExpiredPayRates() - newPayRates.toSet())
    newPayRates.addAll(existingPayRates.expirePayRatesIfMissingFrom(newPayRates))
    newPayRates.adjustStartDatesIfChanged(existingActivity)

    return newPayRates
  }

  private fun MutableList<CourseActivityPayRate>.findExistingPayRate(requested: PayRateRequest) =
    firstOrNull { existing ->
      !existing.hasExpiryDate() &&
        requested.payBand == existing.id.payBandCode &&
        requested.incentiveLevel == existing.id.iepLevelCode
    }

  private fun CourseActivityPayRate.rateIsUnchanged(requested: PayRateRequest) =
    this.halfDayRate.compareTo(CourseActivityPayRate.preciseHalfDayRate(requested.rate)) == 0

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
      .also { expiredPayRates -> expiredPayRates.throwIfPayBandsInUse() }

  private fun List<CourseActivityPayRate>.throwIfPayBandsInUse() =
    this.forEach { activityPayRate ->
      offenderProgramProfileRepository.findByCourseActivity(activityPayRate.id.courseActivity)
        .filter { profile -> profile.isPayRateApplicable(activityPayRate.payBand.code, activityPayRate.iepLevel.code) }
        .map { offender -> offender.offenderBooking.offender.nomsId }
        .toList()
        .takeIf { nomsIds -> nomsIds.isNotEmpty() }
        ?.run { throw BadDataException("Pay band ${activityPayRate.payBand.code} for incentive level ${activityPayRate.iepLevel.code} is allocated to offender(s) $this") }
    }

  private fun PayRateRequest.toCourseActivityPayRate(courseActivity: CourseActivity, startDate: LocalDate, endDate: LocalDate? = null): CourseActivityPayRate {
    val payBand = payBandRepository.findByIdOrNull(PayBand.pk(payBand))
      ?: throw BadDataException("Pay band code $payBand does not exist")

    val availableIepLevel = availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(courseActivity.prison, incentiveLevel)
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

  // If the pay rate already exists the new rate becomes effective tomorrow
  // If the pay rate is brand new it becomes effective today
  private fun getStartDate(iepLevelCode: String, payBandCode: String, existingPayRates: List<CourseActivityPayRate>): LocalDate {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    return existingPayRates.filter { it.id.iepLevelCode == iepLevelCode && it.payBand.code == payBandCode }
      .takeIf { it.isNotEmpty() }
      ?.maxBy { it.id.startDate }
      ?.endDate
      ?.let { if (it < today) today else tomorrow }
      ?: today
  }

  // Handle an edge case where the activity start date is moved before any of the existing pay rates - as start date is
  // part of the key we need to delete and recreate pay rates with the new start date
  private fun MutableList<CourseActivityPayRate>.adjustStartDatesIfChanged(existingActivity: CourseActivity) {
    groupBy { it.iepLevel to it.payBand }
      .map { it.value.minBy { payRate -> payRate.id.startDate } }
      .filter { existingActivity.payRates.containsRate(it) }
      .filter { it.id.startDate > existingActivity.scheduleStartDate }
      .forEach {
        remove(it)
        add(
          PayRateRequest(it.id.iepLevelCode, it.id.payBandCode, it.halfDayRate)
            .toCourseActivityPayRate(
              courseActivity = existingActivity,
              startDate = existingActivity.scheduleStartDate,
            ),
        )
      }
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

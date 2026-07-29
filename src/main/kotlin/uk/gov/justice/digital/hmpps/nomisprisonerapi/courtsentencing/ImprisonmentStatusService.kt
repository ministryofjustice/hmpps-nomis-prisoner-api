package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatusId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
@Profile("imprisonment-status-direct")
class ImprisonmentStatusService(
  private val offenderImprisonmentStatusRepository: OffenderImprisonmentStatusRepository,
  private val imprisonmentStatusRepository: ImprisonmentStatusRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderChargeRepository: OffenderChargeRepository,
  private val courtEventChargeRepository: CourtEventChargeRepository,
) : ImprisonmentStatusServiceProxy {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    enum class ChangeType(val description: String) {
      UPDATE_SENTENCE("DPS Auto created - Updated sentence type or term or related offence type."),
      UPDATE_RESULT("DPS Auto created - Updated offence outcome result."),
      DELETE("DPS Auto created - Deleted Sentence/Offence/Events."),
    }
  }

  fun recalculateImprisonmentStatusAndMainOffence(bookingId: Long, reason: ChangeType): ImprisonmentStatusAndMainOffence {
    val booking = offenderBookingRepository.findByIdWaitForLock(bookingId)
    val offenderNo = booking.offender.nomsId
    val status = statusAndMainOffence(booking, offenderNo)

    val currentStatuses = booking.imprisonmentStatuses
    val activeStatus = currentStatuses.findLast { it.latestStatus }
    if (activeStatus?.statusCode == status.imprisonmentStatus) {
      log.info("No change to imprisonment status for offenderNo $offenderNo, current status: ${activeStatus.statusCode}, new status: ${status.imprisonmentStatus}")
    } else {
      updateImprisonmentStatus(currentStatuses, booking, status.imprisonmentStatus, reason)
    }

    if (status.offenderChargeId != null) {
      updateMainOffence(booking, status.offenderChargeId)
    }

    return status
  }

  private fun updateImprisonmentStatus(
    currentStatuses: MutableList<OffenderImprisonmentStatus>,
    booking: OffenderBooking,
    imprisonmentStatus: String,
    reason: ChangeType,
  ) {
    currentStatuses.filter { it.latestStatus }.forEach {
      // try to acquire lock
      offenderImprisonmentStatusRepository.findByIdWaitForLock(it.id)
      it.latestStatus = false
      it.expiryDate = LocalDate.now()
    }
    offenderImprisonmentStatusRepository.saveAndFlush(
      OffenderImprisonmentStatus(
        id = OffenderImprisonmentStatusId(
          offenderBooking = booking,
          sequence = (currentStatuses.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1,
        ),
        statusCode = imprisonmentStatus,
        status = imprisonmentStatusRepository.findByCode(imprisonmentStatus),
        effectiveDate = LocalDate.now(),
        effectiveTime = LocalDateTime.now(),
        expiryDate = null,
        prison = booking.location,
        createDate = LocalDate.now(),
        commentText = reason.description,
        latestStatus = true,
      ),
    ).also { booking.imprisonmentStatuses.add(it) }
  }

  private fun statusAndMainOffence(
    booking: OffenderBooking,
    offenderNo: String,
  ): ImprisonmentStatusAndMainOffence = (
    offenderImprisonmentStatusRepository.getStatusAndMainOffenceViaSentenceByBookingId(booking.bookingId)?.also {
      log.info("Recalculated imprisonment status for offenderNo by sentence $offenderNo: $it")
    } ?: run {
      offenderImprisonmentStatusRepository.getStatusAndMainOffenceViaChargeOutcomeByBookingId(booking.bookingId).also {
        log.info("Recalculated imprisonment status for offenderNo by charge outcome $offenderNo: $it")
      }
    }
    ).let {
    ImprisonmentStatusAndMainOffence(
      imprisonmentStatus = it.imprisonmentStatus,
      offenderChargeId = it.offenderChargeId?.toLong(),
    )
  }

  private fun updateMainOffence(booking: OffenderBooking, offenderChargeId: Long) {
    resetMainOffenceForOldCharge(booking, offenderChargeId)

    val newMainOffenceCharge = booking.courtCases.flatMap { it.offenderCharges }.first { it.id == offenderChargeId }
    // try to acquire lock
    offenderChargeRepository.findByIdWaitForLock(newMainOffenceCharge.id)
    newMainOffenceCharge.mostSeriousFlag = true
    val courtEventCharges = booking.courtCases.flatMap { it.courtEvents }.flatMap { it.courtEventCharges }.filter { it.id.offenderCharge == newMainOffenceCharge }
    courtEventCharges.forEach {
      courtEventChargeRepository.findByIdWaitForLock(it.id)
      it.mostSeriousFlag = true
    }
  }

  private fun resetMainOffenceForOldCharge(booking: OffenderBooking, offenderChargeId: Long) {
    val currentMainOffenceCharges = booking.courtCases.flatMap { it.offenderCharges }.filter { it.mostSeriousFlag }.filter { it.id != offenderChargeId }
    currentMainOffenceCharges.forEach { offenderCharge ->
      offenderChargeRepository.findByIdWaitForLock(offenderCharge.id)
      offenderCharge.mostSeriousFlag = false
      val courtEventCharges = booking.courtCases.flatMap { it.courtEvents }.flatMap { it.courtEventCharges }.filter { it.id.offenderCharge == offenderCharge }
      courtEventCharges.forEach {
        courtEventChargeRepository.findByIdWaitForLock(it.id)
        it.mostSeriousFlag = false
      }
    }
  }

  override fun recalculateImprisonmentStatusAndMainOffence(
    bookingId: Long,
    changeType: String,
  ) {
    recalculateImprisonmentStatusAndMainOffence(bookingId, ChangeType.valueOf(changeType))
  }
}

data class ImprisonmentStatusAndMainOffence(val imprisonmentStatus: String, val offenderChargeId: Long?)

interface ImprisonmentStatusServiceProxy {
  fun recalculateImprisonmentStatusAndMainOffence(
    bookingId: Long,
    changeType: String,
  )
}

@Service
@Profile("!imprisonment-status-direct")
class StoredProcedureImprisonmentStatusService(
  private val storedProcedureRepository: StoredProcedureRepository,
  private val jdbcTemplate: JdbcTemplate,
) : ImprisonmentStatusServiceProxy {
  override fun recalculateImprisonmentStatusAndMainOffence(
    bookingId: Long,
    changeType: String,
  ) {
    storedProcedureRepository.imprisonmentStatusUpdate(
      jdbcTemplate = jdbcTemplate,
      bookingId = bookingId,
      changeType = changeType,
    )
  }
}

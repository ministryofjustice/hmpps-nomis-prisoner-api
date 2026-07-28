package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatusId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StatusAndMainOffence
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ImprisonmentStatusService(
  private val offenderImprisonmentStatusRepository: OffenderImprisonmentStatusRepository,
  private val imprisonmentStatusRepository: ImprisonmentStatusRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    enum class ChangeType(val description: String) {
      UPDATE_SENTENCE("DPS Auto created - Updated offence outcome result."),
      UPDATE_RESULT("DPS Auto created - Updated sentence type or term or related offence type."),
      DELETE("DPS Auto created - Deleted Sentence/Offence/Events."),
    }
  }

  fun recalculateImprisonmentStatus(offenderNo: String, reason: ChangeType) {
    val booking = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).firstOrNull()
      ?: throw NotFoundException("No booking found for offenderNo $offenderNo")
    val status = statusAndMainOffence(booking, offenderNo)

    val currentStatuses = booking.imprisonmentStatuses
    val activeStatus = currentStatuses.findLast { it.latestStatus }
    if (activeStatus?.statusCode == status.imprisonmentStatus) {
      log.info("No change to imprisonment status for offenderNo $offenderNo, current status: ${activeStatus.statusCode}, new status: ${status.imprisonmentStatus}")
    } else {
      currentStatuses.filter { it.latestStatus }.forEach {
        it.latestStatus = false
        it.expiryDate = LocalDate.now()
      }
      offenderImprisonmentStatusRepository.saveAndFlush(
        OffenderImprisonmentStatus(
          id = OffenderImprisonmentStatusId(
            offenderBooking = booking,
            sequence = (currentStatuses.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1,
          ),
          statusCode = status.imprisonmentStatus,
          status = imprisonmentStatusRepository.findByCode(status.imprisonmentStatus),
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
  }

  private fun statusAndMainOffence(
    booking: OffenderBooking,
    offenderNo: String,
  ): StatusAndMainOffence = offenderImprisonmentStatusRepository.getStatusAndMainOffenceViaSentenceByBookingId(booking.bookingId)?.also {
    log.info("Recalculated imprisonment status for offenderNo by sentence $offenderNo: $it")
  } ?: run {
    offenderImprisonmentStatusRepository.getStatusAndMainOffenceViaChargeOutcomeByBookingId(booking.bookingId).also {
      log.info("Recalculated imprisonment status for offenderNo by charge outcome $offenderNo: $it")
    }
  }
}

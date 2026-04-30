package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class CourtScheduleService(
  private val courtEventRepository: CourtEventRepository,
  private val externalMovementRepository: OffenderExternalMovementRepository,
  private val offenderRepository: OffenderRepository,
) {
  fun getCourtScheduleOut(offenderNo: String, eventId: Long): CourtScheduleOut {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    return courtEventRepository.findByIdOrNull(eventId)
      ?.let {
        CourtScheduleOut(
          bookingId = it.offenderBooking.bookingId,
          eventId = it.id,
          eventDate = it.eventDate,
          startTime = it.startTime,
          eventType = it.courtEventType.code,
          eventStatus = it.eventStatus.code,
          comment = it.commentText,
          prison = findPrisonAt(it.createDatetime, it.offenderBooking.bookingId)?.id
            ?: it.offenderBooking.location.id,
          courtCaseId = it.courtCase?.id,
        )
      }
      ?: throw NotFoundException("Court event with id=$eventId not found for prisoner with nomsId=$offenderNo")
  }

  private fun findPrisonAt(time: LocalDateTime, bookingId: Long): AgencyLocation? {
    val admissions = externalMovementRepository.findAllById_OffenderBooking_BookingId(bookingId)
      .filter { it.movementReason.id.type == "ADM" }
    val admission = admissions.filter { it.movementTime < time }.maxByOrNull { it.movementTime }
      ?: admissions.minByOrNull { it.movementTime }
    return admission?.toAgency
  }
}

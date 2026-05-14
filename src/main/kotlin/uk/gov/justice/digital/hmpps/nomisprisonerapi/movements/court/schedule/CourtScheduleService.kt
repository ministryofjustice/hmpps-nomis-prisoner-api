package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.findActiveCaseloadId

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
      ?.takeIf { it.directionCode?.code == DirectionType.OUT }
      ?.toResponse()
      ?: throw NotFoundException("Court event OUT with id=$eventId not found for prisoner with nomsId=$offenderNo")
  }

  private fun CourtEvent.toResponse() = CourtScheduleOut(
    bookingId = offenderBooking.bookingId,
    eventId = id,
    eventDate = eventDate,
    startTime = startTime,
    eventType = courtEventType.code,
    eventStatus = eventStatus.code,
    comment = commentText,
    prison = externalMovementRepository.findPrisonAt(createDatetime, offenderBooking.bookingId)?.id ?: offenderBooking.location.id,
    court = court.id,
    courtCaseId = courtCase?.id,
    userActiveCaseloadId = findActiveCaseloadId(modifyStaffUserAccount, createStaffUserAccount),
    audit = toAudit(),
  )
}

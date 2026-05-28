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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.findActiveCaseloadId
import java.time.LocalTime

private val DEFAULT_IN_TIME = LocalTime.of(17, 0)

@Transactional(readOnly = true)
@Service
class CourtScheduleService(
  private val courtEventRepository: CourtEventRepository,
  private val externalMovementRepository: OffenderExternalMovementRepository,
  private val offenderRepository: OffenderRepository,
  private val movementHelpers: MovementHelpers,
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

  @Transactional
  fun upsertCourtScheduleOut(offenderNo: String, request: UpsertCourtScheduleOut): UpsertCourtScheduleOutResponse {
    val offenderBooking = movementHelpers.offenderBookingOrThrow(offenderNo)
    val courtEventType = movementHelpers.movementReasonOrThrow(request.eventType)
    val eventStatus = movementHelpers.eventStatusOrThrow(request.eventStatus)
    val returnEventStatus = request.returnStatus?.let { movementHelpers.eventStatusOrThrow(request.returnStatus) }
    val prison = movementHelpers.agencyLocationOrThrow(request.prison)
    val court = movementHelpers.agencyLocationOrThrow(request.court)

    val scheduleOut = request.eventId
      ?.let { courtEventRepository.findById(it).get() }
      ?. apply {
        this.eventDate = request.startTime.toLocalDate()
        this.startTime = request.startTime
        this.courtEventType = courtEventType
        this.eventStatus = eventStatus
        this.commentText = request.comment
        this.court = court
      }
      ?: CourtEvent(
        offenderBooking = offenderBooking,
        eventDate = request.startTime.toLocalDate(),
        startTime = request.startTime,
        courtEventType = courtEventType,
        eventStatus = eventStatus,
        commentText = request.comment,
        court = court,
        directionCode = movementHelpers.directionTypeOrThrow(DirectionType.OUT),
      )
    val savedScheduleOut = courtEventRepository.saveAndFlush(scheduleOut)

    val existingScheduleIn = request.eventId?.let { courtEventRepository.findByParentEventId(request.eventId) }
    if (returnEventStatus != null) {
      val scheduleIn = existingScheduleIn?.apply {
        this.parentEventId = savedScheduleOut.id
        this.eventDate = request.startTime.toLocalDate()
        this.startTime = request.startTime.with(DEFAULT_IN_TIME)
        this.courtEventType = courtEventType
        this.eventStatus = eventStatus
        this.commentText = request.comment
        this.court = prison
      } ?: CourtEvent(
        offenderBooking = offenderBooking,
        parentEventId = savedScheduleOut.id,
        eventDate = request.startTime.toLocalDate(),
        startTime = request.startTime.with(DEFAULT_IN_TIME),
        courtEventType = courtEventType,
        eventStatus = returnEventStatus,
        commentText = request.comment,
        court = prison,
        directionCode = movementHelpers.directionTypeOrThrow(DirectionType.IN),
      )
      courtEventRepository.save(scheduleIn)
    } else {
      if (existingScheduleIn != null) {
        courtEventRepository.delete(existingScheduleIn)
      }
    }

    return UpsertCourtScheduleOutResponse(offenderBooking.bookingId, scheduleOut.id)
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

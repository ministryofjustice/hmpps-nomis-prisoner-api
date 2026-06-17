package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.COMPLETED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers.Companion.MAX_COURT_SCHEDULER_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.findActiveCaseloadId
import java.time.LocalTime

private val DEFAULT_IN_TIME = LocalTime.of(17, 0)

@Transactional(readOnly = true)
@Service
class CourtScheduleService(
  private val courtEventRepository: CourtEventRepository,
  private val externalMovementRepository: OffenderExternalMovementRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val movementOutRepository: OffenderCourtMovementOutRepository,
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
  @Audit(auditModule = "DPS_COURT_SCHEDULER_SYNCHRONISATION")
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
        this.setEventDateAndTime(request.startTime)
        this.courtEventType = courtEventType
        this.eventStatus = eventStatus
        this.commentText = request.comment?.truncateToUtf8Length(MAX_COURT_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true)
        this.court = court
      }
      ?: CourtEvent(
        offenderBooking = offenderBooking,
        eventDate = request.startTime.toLocalDate(),
        startTime = request.startTime,
        courtEventType = courtEventType,
        eventStatus = eventStatus,
        commentText = request.comment?.truncateToUtf8Length(MAX_COURT_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true),
        court = court,
        directionCode = movementHelpers.directionTypeOrThrow(DirectionType.OUT),
      )
    val savedScheduleOut = courtEventRepository.saveAndFlush(scheduleOut)

    val existingScheduleIn = request.eventId?.let { courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(offenderBooking.bookingId, request.eventId) }
    if (returnEventStatus != null) {
      val scheduleIn = existingScheduleIn?.apply {
        this.parentEventId = savedScheduleOut.id
        this.setEventDateAndTime(request.startTime.with(DEFAULT_IN_TIME))
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

  @Transactional
  @Audit(auditModule = "DPS_COURT_SCHEDULER_SYNCHRONISATION")
  fun deleteCourtScheduleOut(offenderNo: String, eventId: Long) {
    courtEventRepository.findByIdOrNull(eventId)
      ?.also { schedule ->
        movementOutRepository.findByCourtScheduleOutId(eventId)
          .takeIf { it != null }
          ?.run { throw ConflictException("Cannot delete court schedule out eventId $eventId because it has a movement ${this.id.offenderBooking.bookingId} / ${this.id.sequence}") }

        if (schedule.eventStatus.code == COMPLETED) {
          throw ConflictException("Cannot delete court schedule out eventId $eventId because it has status $COMPLETED")
        }

        courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(schedule.offenderBooking.bookingId, eventId)
          .takeIf { it != null }
          ?.run { throw ConflictException("Cannot delete court schedule out eventId $eventId because it has an inbound schedule ${this.id}") }

        offenderBookingRepository.findByIdOrNull(schedule.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("EventId $eventId exists on a different offender") }

        courtEventRepository.delete(schedule)
      }
  }

  private fun CourtEvent.toResponse() = CourtScheduleOut(
    bookingId = offenderBooking.bookingId,
    eventId = id,
    eventDate = eventDate,
    startTime = getEventDateAndTime(),
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

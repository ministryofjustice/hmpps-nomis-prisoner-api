package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.service.ExternalMovementService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers.Companion.MAX_COURT_SCHEDULER_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.findActiveCaseloadId
import java.time.LocalTime

private val DEFAULT_IN_TIME = LocalTime.of(17, 0)

@Transactional(readOnly = true)
@Service
class CourtScheduleService(
  private val courtEventRepository: CourtEventRepository,
  private val externalMovementService: ExternalMovementService,
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
      ?.takeIf { it.directionCode == null || it.directionCode?.code == DirectionType.OUT }
      ?.toResponse()
      ?: throw NotFoundException("Court event OUT with id=$eventId not found for prisoner with nomsId=$offenderNo")
  }

  @Transactional
  @Audit(auditModule = "DPS_COURT_SCHEDULER_SYNCHRONISATION")
  fun upsertCourtScheduleOut(
    offenderNo: String,
    request: UpsertCourtScheduleOut,
    recreate: Boolean,
  ): UpsertCourtScheduleOutResponse {
    if (recreate && request.eventId == null) throw BadDataException("Cannot recreate a court schedule out without an eventId")

    val offenderBooking = movementHelpers.offenderBookingOrThrow(offenderNo)
    val courtEventType = movementHelpers.movementReasonOrThrow(request.eventType)
    val eventStatus = movementHelpers.eventStatusOrThrow(request.eventStatus)
    val returnEventStatus = request.returnStatus?.let { movementHelpers.eventStatusOrThrow(request.returnStatus) }
    val court = movementHelpers.agencyLocationOrThrow(request.court)

    val scheduleOut = request.eventId
      ?.takeIf { !recreate }
      ?.let { courtEventRepository.findByIdOrNullForUpdate(it)!! }
      ?. apply {
        this.setEventDateAndTime(request.startTime)
        this.courtEventType = courtEventType
        this.eventStatus = eventStatus
        this.commentText = request.comment?.truncateToUtf8Length(MAX_COURT_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true)
        this.court = court
      }
      ?: CourtEvent(
        id = if (recreate) request.eventId!! else 0,
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
        this.eventStatus = returnEventStatus
        this.commentText = request.comment
        this.court = offenderBooking.location
      } ?: CourtEvent(
        offenderBooking = offenderBooking,
        parentEventId = savedScheduleOut.id,
        eventDate = request.startTime.toLocalDate(),
        startTime = request.startTime.with(DEFAULT_IN_TIME),
        courtEventType = courtEventType,
        eventStatus = returnEventStatus,
        commentText = request.comment,
        court = offenderBooking.location,
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
    courtEventRepository.findByIdOrNullForUpdate(eventId)
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
    latestBooking = offenderBooking.bookingSequence == 1,
    eventId = id,
    eventDate = eventDate,
    startTime = getEventDateAndTime(),
    eventType = courtEventType.code,
    eventStatus = eventStatus.code,
    comment = commentText,
    prison = externalMovementService.findPrisonAt(getEventDateAndTime(), offenderBooking.offender.nomsId)?.id ?: offenderBooking.location.id,
    court = court.id,
    courtCaseId = courtCase?.id,
    userActiveCaseloadId = findActiveCaseloadId(modifyStaffUserAccount, createStaffUserAccount),
    audit = toAudit(),
  )
}

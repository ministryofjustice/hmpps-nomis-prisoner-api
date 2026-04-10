package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.COMPLETED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapAddressService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.toFullAddress
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class TapScheduleService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val scheduleOutRepository: OffenderTapScheduleOutRepository,
  private val scheduleInRepository: OffenderTapScheduleInRepository,
  private val tapAddressService: TapAddressService,
  private val tapHelpers: TapHelpers,
) {

  fun getTapScheduleOut(offenderNo: String, eventId: Long): TapScheduleOut {
    tapHelpers.offenderOrThrow(offenderNo)

    val tapScheduleOut = scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Tap scheduleout with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return tapScheduleOut.toResponse(tapScheduleOut.tapApplication.returnTime)
  }

  @Transactional
  fun upsertTapScheduleOut(offenderNo: String, request: UpsertTapScheduleOut): UpsertTapScheduleOutResponse {
    val offenderBooking = tapHelpers.offenderBookingOrThrow(offenderNo)
    val application = tapHelpers.movementApplicationOrThrow(request.tapApplicationId)
    val eventSubType = tapHelpers.movementReasonOrThrow(request.eventSubType)
    val eventStatus = tapHelpers.eventStatusOrThrow(request.eventStatus)
    val escort = request.escort?.let { tapHelpers.escortOrThrow(request.escort) }
    val fromPrison = tapHelpers.agencyLocationOrThrow(request.fromPrison)
    val toAgency = request.toAgency?.let { tapHelpers.agencyLocationOrThrow(request.toAgency) }
    val transportType = request.transportType?.let { tapHelpers.transportTypeOrThrow(request.transportType) }
    val toAddress = tapAddressService.findAddressOrThrow(request.toAddress, offenderBooking.offender)

    if (request.eventId == null) {
      if (application.isUnapproved()) {
        throw ConflictException("Cannot add schedules to application ${application.tapApplicationId} because it's not approved (applicationStatus=${application.applicationStatus.code})")
      }
      if (application.isSingle() && application.hasSchedules()) {
        throw ConflictException("Cannot add schedules to application ${application.tapApplicationId} because it already has a single schedule")
      }
    }

    val schedule = request.eventId
      ?.let { scheduleOutRepository.findById(request.eventId).get() }
      ?.apply {
        this.eventDate = request.eventDate
        this.startTime = request.startTime
        this.eventSubType = eventSubType
        this.eventStatus = eventStatus
        this.comment = request.comment?.truncateToUtf8Length(MAX_TAP_COMMENT_LENGTH, includeSeeDpsSuffix = true)
        this.escort = escort
        this.fromAgency = fromPrison
        this.toAgency = toAgency
        this.transportType = transportType
        this.returnDate = request.returnDate
        this.returnTime = request.returnTime
        this.toAddressOwnerClass = toAddress.addressOwnerClass
        this.toAddress = toAddress
      }
      ?: OffenderTapScheduleOut(
        offenderBooking = offenderBooking,
        eventDate = request.eventDate,
        startTime = request.startTime,
        eventSubType = eventSubType,
        eventStatus = eventStatus,
        comment = request.comment?.truncateToUtf8Length(MAX_TAP_COMMENT_LENGTH, includeSeeDpsSuffix = true),
        escort = escort,
        fromPrison = fromPrison,
        toAgency = toAgency,
        transportType = transportType,
        returnDate = request.returnDate,
        returnTime = request.returnTime,
        tapApplication = application,
        toAddressOwnerClass = toAddress.addressOwnerClass,
        toAddress = toAddress,
        applicationDate = request.applicationDate,
        applicationTime = request.applicationTime,
      )

    val scheduledReturn = schedule.tapScheduleIns.firstOrNull()
      ?.apply {
        this.eventStatus = tapHelpers.eventStatusOrThrow(request.returnEventStatus ?: "SCH")
        this.eventDate = request.returnDate
        this.startTime = request.returnTime
        this.eventSubType = eventSubType
        this.escort = escort
        this.fromAgency = toAgency
        this.toAgency = fromPrison
      }
      ?: let {
        if (request.eventStatus == "COMP") {
          OffenderTapScheduleIn(
            offenderBooking = offenderBooking,
            tapApplication = application,
            tapScheduleOut = schedule,
            eventStatus = tapHelpers.eventStatusOrThrow(request.returnEventStatus ?: "SCH"),
            eventDate = request.returnDate,
            startTime = request.returnTime,
            eventSubType = eventSubType,
            escort = escort,
            fromAgency = toAgency,
            toPrison = fromPrison,
          )
        } else {
          null
        }
      }

    schedule.tapScheduleIns = scheduledReturn?.let { mutableListOf(scheduledReturn) } ?: mutableListOf()

    scheduleOutRepository.save(schedule)

    return UpsertTapScheduleOutResponse(
      bookingId = offenderBooking.bookingId,
      tapApplicationId = application.tapApplicationId,
      eventId = schedule.eventId,
      addressId = toAddress.addressId,
      addressOwnerClass = toAddress.addressOwnerClass,
    )
  }

  @Transactional
  fun deleteTapScheduleOut(offenderNo: String, eventId: Long) {
    scheduleOutRepository.findByIdOrNull(eventId)
      ?.also { schedule ->
        if (schedule.tapMovementOut != null) {
          throw ConflictException("Cannot delete tap schedule out eventId $eventId because it has a movement ${schedule.tapMovementOut!!.id.offenderBooking.bookingId} / ${schedule.tapMovementOut!!.id.sequence}}")
        }
        if (schedule.eventStatus.code == COMPLETED) {
          throw ConflictException("Cannot delete tap schedule out eventId $eventId because it has status $COMPLETED")
        }
        if (schedule.tapScheduleIns.isNotEmpty()) {
          throw ConflictException("Cannot delete tap schedule out eventId $eventId because it has an inbound schedule ${schedule.tapScheduleIns[0].eventId}")
        }

        offenderBookingRepository.findByIdOrNull(schedule.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("EventId $eventId exists on a different offender") }

        scheduleOutRepository.delete(schedule)
      }
  }

  fun getTapScheduleIn(offenderNo: String, eventId: Long): TapScheduleIn {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val tapScheduleIn = scheduleInRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Tap schedule in with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return tapScheduleIn.toResponse()
  }

  private fun OffenderTapScheduleOut.toResponse(applicationReturnTime: LocalDateTime) = TapScheduleOut(
    bookingId = offenderBooking.bookingId,
    tapApplicationId = tapApplication.tapApplicationId,
    eventId = eventId,
    eventDate = eventDate ?: tapApplication.fromDate,
    startTime = startTime ?: tapApplication.releaseTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    inboundEventStatus = tapScheduleIns.firstOrNull()?.eventStatus?.code,
    comment = comment,
    escort = escort?.code,
    fromPrison = fromAgency?.id,
    toAgency = toAgency?.id,
    transportType = transportType?.code,
    returnDate = returnDate,
    returnTime = returnTime ?: applicationReturnTime,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    toAddressDescription = tapAddressService.getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(tapAddressService.getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    contactPersonName = contactPersonName,
    tapAbsenceType = tapApplication.tapType?.code,
    tapSubType = tapApplication.tapSubType?.code,
    audit = toAudit(
      toAddress?.modifyDatetime?.takeIf { it.isAfter(modifyDatetime ?: createDatetime) },
      toAddress?.modifyDatetime?.takeIf { it.isAfter(modifyDatetime ?: createDatetime) }?.let { toAddress?.modifyUserId },
    ),
  )

  private fun OffenderTapScheduleIn.toResponse() = TapScheduleIn(
    bookingId = offenderBooking.bookingId,
    tapApplicationId = tapScheduleOut.tapApplication.tapApplicationId,
    eventId = eventId,
    parentEventId = tapScheduleOut.eventId,
    eventDate = eventDate ?: tapScheduleOut.tapApplication.fromDate,
    startTime = startTime ?: tapScheduleOut.tapApplication.releaseTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    comment = comment,
    escort = escort?.code,
    fromAgency = fromAgency?.id,
    toPrison = toAgency?.id,
    audit = toAudit(),
  )
}

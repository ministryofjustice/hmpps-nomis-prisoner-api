package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapAddressService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.tapMovementIns
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.tapMovementOuts
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.toFullAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.unlinkWrongSchedules
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class OffenderTapsService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val tapApplicationRepository: OffenderTapApplicationRepository,
  private val tapScheduleOutRepository: OffenderTapScheduleOutRepository,
  private val tapMovementOutRepository: OffenderTapMovementOutRepository,
  private val tapMovementInRepository: OffenderTapMovementInRepository,
  private val externalMovementsRepository: OffenderExternalMovementRepository,
  private val tapHelpers: TapHelpers,
  private val tapAddressService: TapAddressService,
) {

  fun getOffenderTaps(offenderNo: String): OffenderTapsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    // We need to run these queries before findAllByOffenderBooking_Offender_NomsId otherwise if Hibernate finds an entity of
    // type OffenderTapMovementOut in the session where it's expecting an OffenderTapMovementIn it tries (and fails)
    // to use the wrong type instead of respecting the @NotFound(IGNORE). So we run these queries before the entity is loaded
    // into the session and the @NotFound is respected.
    val allTapMovementOuts = tapMovementOutRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    val allTapMovementIns = tapMovementInRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)

    val tapApplications = tapApplicationRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .also { it.fixMergedSchedules() }
      .also { it.unlinkCorruptTapMovementIns() }

    // To find the unscheduled movements we get all movements and remove those linked to a TAP application. This is necessary because of corrupt movements linked to the wrong application.
    val unscheduledTapMovementOuts = allTapMovementOuts - tapApplications.tapMovementOuts()
    val unscheduledTapMovementIns = allTapMovementIns - tapApplications.tapMovementIns()

    data class Booking(val id: Long, val active: Boolean, val latest: Boolean)

    val bookings = (
      tapApplications.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        unscheduledTapMovementOuts.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        unscheduledTapMovementIns.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) }
      ).toSet()

    return OffenderTapsResponse(
      bookings = bookings.map { (bookingId, active, latest) ->
        toBookingTaps(
          bookingId = bookingId,
          active = active,
          latest = latest,
          tapApplications = tapApplications,
          unscheduledTapMovementOuts = unscheduledTapMovementOuts,
          unscheduledTapMovementIns = unscheduledTapMovementIns,
        )
      },
    )
  }

  fun getBookingTaps(bookingId: Long): BookingTaps {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")

    // We need to run these queries before findAllByOffenderBooking_BookingId otherwise if Hibernate finds an entity of
    // type OffenderTapMovementOut in the session where it's expecting an OffenderTapMovementIn it tries (and fails)
    // to use the wrong type instead of respecting the @NotFound(IGNORE). So we run these queries before the entity is loaded
    // into the session and the @NotFound is respected.
    val allTapMovementOuts = tapMovementOutRepository.findAllByOffenderBooking_BookingId(bookingId)
    val allTapMovementIns = tapMovementInRepository.findAllByOffenderBooking_BookingId(bookingId)

    val movementApplications = tapApplicationRepository.findAllByOffenderBooking_BookingId(bookingId)
      .also { it.fixMergedSchedules() }
      .also { it.unlinkCorruptTapMovementIns() }

    // To find the unscheduled movements we get all movements and remove those linked to a TAP application. This is necessary because of corrupt movements linked to the wrong application.
    val unscheduledTapMovementOuts = allTapMovementOuts - movementApplications.tapMovementOuts()
    val unscheduledTapMovementIns = allTapMovementIns - movementApplications.tapMovementIns()

    return toBookingTaps(
      bookingId = bookingId,
      active = booking.active,
      latest = booking.bookingSequence == 1,
      tapApplications = movementApplications,
      unscheduledTapMovementOuts = unscheduledTapMovementOuts,
      unscheduledTapMovementIns = unscheduledTapMovementIns,
    )
  }

  fun getTapsSummary(offenderNo: String): TapSummary {
    tapHelpers.offenderOrThrow(offenderNo)
    val scheduledIn = externalMovementsRepository.countOffenderScheduledIn(offenderNo)
    val scheduledOut = externalMovementsRepository.countOffenderScheduledOut(offenderNo)
    val unscheduledIn = externalMovementsRepository.countOffenderUnscheduledIn(offenderNo)
    val unscheduledOut = externalMovementsRepository.countOffenderUnscheduledOut(offenderNo)
    return TapSummary(
      applications = ApplicationSummary(count = tapApplicationRepository.countByOffenderBooking_Offender_NomsId(offenderNo)),
      scheduledOuts = ScheduledOutSummary(count = tapScheduleOutRepository.countByOffenderBooking_Offender_NomsId_AndTapApplicationIsNotNull(offenderNo)),
      movements = MovementSummary(
        count = scheduledIn + scheduledOut + unscheduledIn + unscheduledOut,
        scheduled = MovementsByDirection(outCount = scheduledOut, inCount = scheduledIn),
        unscheduled = MovementsByDirection(outCount = unscheduledOut, inCount = unscheduledIn),
      ),
    )
  }

  fun getTapsIds(offenderNo: String): OffenderTapsIdsResponse = getOffenderTaps(offenderNo).let {
    OffenderTapsIdsResponse(
      applicationIds = it.bookings.flatMap { it.tapApplications.map { it.tapApplicationId } },
      scheduleOutIds = it.bookings.flatMap {
        it.tapApplications.flatMap {
          it.taps.flatMap {
            listOfNotNull(
              it.tapScheduleOut?.eventId,
            )
          }
        }
      },
      scheduleInIds = it.bookings.flatMap {
        it.tapApplications.flatMap {
          it.taps.flatMap {
            listOfNotNull(
              it.tapScheduleIn?.eventId,
            )
          }
        }
      },
      scheduledMovementOutIds = it.bookings.flatMap { booking ->
        booking.tapApplications.flatMap {
          it.taps.flatMap {
            listOfNotNull(
              it.tapMovementOut?.let { OffenderTapMovementId(booking.bookingId, it.sequence) },
            )
          }
        }
      },
      scheduledMovementInIds = it.bookings.flatMap { booking ->
        booking.tapApplications.flatMap {
          it.taps.flatMap {
            listOfNotNull(
              it.tapMovementIn?.let { OffenderTapMovementId(booking.bookingId, it.sequence) },
            )
          }
        }
      },
      unscheduledMovementOutIds = it.bookings.flatMap { booking ->
        booking.unscheduledTapMovementOuts.map {
          OffenderTapMovementId(
            booking.bookingId,
            it.sequence,
          )
        }
      },
      unscheduledMovementInIds = it.bookings.flatMap { booking ->
        booking.unscheduledTapMovementIns.map {
          OffenderTapMovementId(
            booking.bookingId,
            it.sequence,
          )
        }
      },
    )
  }

  private fun List<OffenderTapApplication>.fixMergedSchedules() {
    // find any tap schedule ins on the wrong application and remove them (these are created during merges)
    val tapScheduleInsWithWrongParent = this.flatMap { application ->
      application.tapScheduleOuts.flatMap { it.tapScheduleIns }
        .filter { it.tapApplication != null && it.tapApplication != application }
    }

    // detach the tap schedule ins with wrong application from their scheduled tap
    this.flatMap { it.tapScheduleOuts }.forEach {
      it.tapScheduleIns.removeAll(tapScheduleInsWithWrongParent)
    }

    // put the tap schedule ins onto the correct application and schedule out
    tapScheduleInsWithWrongParent.forEach { mergedTapScheduleIn ->
      this.find { it.tapApplicationId == mergedTapScheduleIn.tapApplication?.tapApplicationId }
        ?.tapScheduleOuts
        ?.firstOrNull { scheduleOut -> scheduleOut.tapScheduleIns.isEmpty() && scheduleOut.returnDate == mergedTapScheduleIn.eventDate }
        ?.tapScheduleIns += mergedTapScheduleIn
      mergedTapScheduleIn.tapApplication = null
    }
  }

  /*
   * Cut links to any tap movement ins that have ended up on the wrong application due to corrupt data
   */
  private fun List<OffenderTapApplication>.unlinkCorruptTapMovementIns() {
    forEach {
      it.tapScheduleOuts.forEach {
        it.tapScheduleIns.forEach { tapScheduleIn ->
          tapScheduleIn.tapMovementIn?.also { tapMovementIn ->
            if (tapMovementIn.unlinkWrongSchedules()) {
              tapScheduleIn.tapMovementIn = null
            }
          }
        }
      }
    }
  }

  // Make a tap movement in unscheduled if the schedules have been corrupted
  private fun toBookingTaps(
    bookingId: Long,
    active: Boolean,
    latest: Boolean,
    tapApplications: List<OffenderTapApplication>,
    unscheduledTapMovementOuts: List<OffenderTapMovementOut>,
    unscheduledTapMovementIns: List<OffenderTapMovementIn>,
  ) = BookingTaps(
    bookingId = bookingId,
    activeBooking = active,
    latestBooking = latest,
    tapApplications = tapApplications.filter { it.offenderBooking.bookingId == bookingId }
      .map { it.toResponse() },
    unscheduledTapMovementOuts = unscheduledTapMovementOuts.filter { it.offenderBooking.bookingId == bookingId }
      .map { mov -> mov.toUnscheduledResponse() },
    unscheduledTapMovementIns = unscheduledTapMovementIns.filter { it.offenderBooking.bookingId == bookingId }
      .map { mov -> mov.toUnscheduledResponse() },
  )

  private fun OffenderTapApplication.toResponse() = BookingTapApplication(
    tapApplicationId = tapApplicationId,
    eventSubType = eventSubType.code,
    applicationDate = applicationDate.toLocalDate(),
    fromDate = fromDate,
    releaseTime = releaseTime,
    toDate = toDate,
    returnTime = returnTime,
    applicationStatus = applicationStatus.code,
    escortCode = escort?.code,
    transportType = transportType?.code,
    comment = comment,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    toAddressDescription = tapAddressService.getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(tapAddressService.getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    prisonId = prison.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    tapType = tapType?.code,
    tapSubType = tapSubType?.code,
    taps = tapScheduleOuts.map {
      // Some old data has multiple scheduled IN movements for a single scheduled OUT - try to find the one with an actual movement IN
      val tapScheduleIn = it.tapScheduleIns.firstOrNull { it.tapMovementIn != null }
        ?: it.tapScheduleIns.firstOrNull()
      val tapMovementIn = tapScheduleIn?.tapMovementIn
      BookingTap(
        tapScheduleOut = it.toResponse(returnTime),
        tapScheduleIn = tapScheduleIn?.toResponse(),
        tapMovementOut = it.tapMovementOut?.toResponse(),
        tapMovementIn = tapMovementIn?.toResponse(),
      )
    },
    audit = toAudit(),
  )

  private fun OffenderTapScheduleOut.toResponse(applicationReturnTime: LocalDateTime) = BookingTapScheduleOut(
    eventId = eventId,
    eventDate = eventDate ?: tapApplication.fromDate,
    startTime = startTime ?: tapApplication.releaseTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
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
    audit = toAudit(),
  )

  private fun OffenderTapScheduleIn.toResponse() = BookingTapScheduleIn(
    eventId = eventId,
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

  private fun OffenderTapMovementOut.toUnscheduledResponse(): BookingTapMovementOut {
    val address = toAddress ?: toAgency?.addresses?.firstOrNull()
    return BookingTapMovementOut(
      sequence = id.sequence,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.id.reasonCode,
      arrestAgency = arrestAgency?.code,
      escort = escort?.code,
      escortText = escortText,
      fromPrison = fromAgency?.id,
      toAgency = toAgency?.id,
      commentText = commentText,
      toAddressId = address?.addressId,
      toAddressOwnerClass = address?.addressOwnerClass,
      toAddressDescription = tapAddressService.getAddressDescription(address),
      toFullAddress = address?.toFullAddress(tapAddressService.getAddressDescription(address)) ?: toCity?.description,
      toAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTapMovementIn.toUnscheduledResponse(): BookingTapMovementIn {
    val address = fromAddress ?: fromAgency?.addresses?.firstOrNull()
    return BookingTapMovementIn(
      sequence = id.sequence,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.id.reasonCode,
      escort = escort?.code,
      escortText = escortText,
      fromAgency = fromAgency?.id,
      toPrison = toAgency?.id,
      commentText = commentText,
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = tapAddressService.getAddressDescription(address),
      fromFullAddress = address?.toFullAddress(tapAddressService.getAddressDescription(fromAddress)) ?: fromCity?.description,
      fromAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTapMovementOut.toResponse(): BookingTapMovementOut {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress ?: tapScheduleOut?.toAddress
    return BookingTapMovementOut(
      sequence = id.sequence,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.id.reasonCode,
      arrestAgency = arrestAgency?.code,
      escort = escort?.code,
      escortText = escortText,
      fromPrison = fromAgency?.id,
      toAgency = toAgency?.id,
      commentText = commentText,
      toAddressId = toAddress?.addressId,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      toAddressDescription = tapAddressService.getAddressDescription(toAddress),
      toFullAddress = toAddress?.toFullAddress(tapAddressService.getAddressDescription(toAddress)),
      toAddressPostcode = toAddress?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTapMovementIn.toResponse(): BookingTapMovementIn {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: tapScheduleOut?.tapMovementOut?.toAddress
      ?: tapScheduleOut?.toAddress
    return BookingTapMovementIn(
      sequence = id.sequence,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.id.reasonCode,
      escort = escort?.code,
      escortText = escortText,
      fromAgency = fromAgency?.id,
      toPrison = toAgency?.id,
      commentText = commentText,
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = tapAddressService.getAddressDescription(address),
      fromFullAddress = address?.toFullAddress(tapAddressService.getAddressDescription(address)),
      fromAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }
}

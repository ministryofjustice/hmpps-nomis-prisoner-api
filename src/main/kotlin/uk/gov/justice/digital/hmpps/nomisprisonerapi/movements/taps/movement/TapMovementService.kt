package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.activeExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.maxMovementSequence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapAddressService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.toFullAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.unlinkWrongSchedules

@Service
@Transactional(readOnly = true)
class TapMovementService(
  private val tapMovementOutRepository: OffenderTapMovementOutRepository,
  private val tapMovementInRepository: OffenderTapMovementInRepository,
  private val tapAddressService: TapAddressService,
  private val tapHelpers: TapHelpers,
  movementTypeRepository: ReferenceCodeRepository<MovementType>,
) {
  private val tapMovementType = movementTypeRepository.findByIdOrNull(MovementType.pk("TAP"))
    ?: throw IllegalStateException("TAP movement type not found")

  fun getTapMovementOut(offenderNo: String, bookingId: Long, movementSeq: Int): TapMovementOut {
    tapHelpers.offenderOrThrow(offenderNo)

    val tapMovementOut = tapMovementOutRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Tap movement out with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    // Despite being non-nullable, Hibernate happily loads a null application if missing in NOMIS - if so then make the movement unscheduled
    if (tapMovementOut.tapScheduleOut?.tapApplication == null) {
      tapMovementOut.tapScheduleOut = null
    }

    return tapMovementOut.toResponse()
  }

  @Transactional
  fun createTapMovementOut(offenderNo: String, request: CreateTapMovementOut): CreateTapMovementOutResponse {
    val offenderBooking = tapHelpers.offenderBookingOrThrow(offenderNo)
    val tapScheduleOut = request.tapScheduleOutId?.let { tapHelpers.tapScheduleOutOrThrow(request.tapScheduleOutId) }
    val movementReason = tapHelpers.movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { tapHelpers.arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { tapHelpers.escortOrThrow(request.escort) }
    val fromPrison = tapHelpers.agencyLocationOrThrow(request.fromPrison)
    val toAgency = request.toAgency?.let { tapHelpers.agencyLocationOrThrow(request.toAgency) }
    val toAddress = request.toAddressId?.let { tapHelpers.addressOrThrow(request.toAddressId) }

    return OffenderTapMovementOut(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      tapScheduleOut = tapScheduleOut,
      movementDate = request.movementDate,
      movementTime = request.movementTime,
      movementType = tapMovementType,
      movementReason = movementReason,
      arrestAgency = arrestAgency,
      escort = escort,
      escortText = request.escortText,
      fromPrison = fromPrison,
      toAgency = toAgency,
      active = true,
      commentText = request.commentText,
      toCity = toAddress?.city,
      toAddress = toAddress,
    ).let {
      offenderBooking.activeExternalMovement().forEach { it.active = false }
      tapMovementOutRepository.save(it)
    }.let {
      CreateTapMovementOutResponse(
        bookingId = offenderBooking.bookingId,
        movementSequence = it.id.sequence,
      )
    }
  }

  fun getTapMovementIn(offenderNo: String, bookingId: Long, movementSeq: Int): TapMovementIn {
    tapHelpers.offenderOrThrow(offenderNo)

    val tapMovementIn = tapMovementInRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Tap movement in with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    // Despite being non-nullable, Hibernate happily loads a null application if missing in NOMIS - if so then make the movement unscheduled
    if (tapMovementIn.tapScheduleOut?.tapApplication == null) {
      tapMovementIn.tapScheduleOut = null
      tapMovementIn.tapScheduleIn = null
    }

    tapMovementIn.unlinkWrongSchedules()
    return tapMovementIn.toResponse()
  }

  @Transactional
  fun createTapMovementIn(offenderNo: String, request: CreateTapMovementIn): CreateTapMovementInResponse {
    val offenderBooking = tapHelpers.offenderBookingOrThrow(offenderNo)
    val tapScheduleIn = request.tapScheduleInId?.let { tapHelpers.tapScheduleInOrThrow(request.tapScheduleInId) }
    val movementReason = tapHelpers.movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { tapHelpers.arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { tapHelpers.escortOrThrow(request.escort) }
    val fromAgency = request.fromAgency?.let { tapHelpers.agencyLocationOrThrow(request.fromAgency) }
    val toPrison = tapHelpers.agencyLocationOrThrow(request.toPrison)
    val fromAddress = request.fromAddressId?.let { tapHelpers.addressOrThrow(request.fromAddressId) }

    return OffenderTapMovementIn(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      tapScheduleIn = tapScheduleIn,
      tapScheduleOut = tapScheduleIn?.tapScheduleOut,
      movementDate = request.movementDate,
      movementTime = request.movementTime,
      movementType = tapMovementType,
      movementReason = movementReason,
      arrestAgency = arrestAgency,
      escort = escort,
      escortText = request.escortText,
      fromAgency = fromAgency,
      toPrison = toPrison,
      active = true,
      commentText = request.commentText,
      fromCity = fromAddress?.city,
      fromAddress = fromAddress,
    ).let {
      offenderBooking.activeExternalMovement().forEach { it.active = false }
      tapMovementInRepository.save(it)
    }.let {
      CreateTapMovementInResponse(
        bookingId = offenderBooking.bookingId,
        movementSequence = it.id.sequence,
      )
    }
  }
  private fun OffenderTapMovementOut.toResponse(): TapMovementOut {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress
      ?: toAgency?.addresses?.firstOrNull()
      ?: tapScheduleOut?.toAddress
    return TapMovementOut(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      tapScheduleOutId = tapScheduleOut?.eventId,
      tapApplicationId = tapScheduleOut?.tapApplication?.tapApplicationId,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.code,
      arrestAgency = arrestAgency?.code,
      escort = escort?.code,
      escortText = escortText,
      fromPrison = fromAgency!!.id,
      toAgency = toAgency?.id,
      commentText = commentText,
      toAddressId = toAddress?.addressId,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      toAddressDescription = tapAddressService.getAddressDescription(toAddress),
      toFullAddress = toAddress?.toFullAddress(tapAddressService.getAddressDescription(toAddress)) ?: toCity?.description,
      toAddressPostcode = toAddress?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTapMovementIn.toResponse(): TapMovementIn {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: fromAgency?.addresses?.firstOrNull()
      ?: tapScheduleOut?.tapMovementOut?.toAddress
      ?: tapScheduleOut?.toAddress
    return TapMovementIn(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      tapScheduleOutId = tapScheduleOut?.eventId,
      tapScheduleInId = tapScheduleIn?.eventId,
      tapApplicationId = tapScheduleOut?.tapApplication?.tapApplicationId,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.code,
      escort = escort?.code,
      escortText = escortText,
      fromAgency = fromAgency?.id,
      toPrison = toAgency!!.id,
      commentText = commentText,
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = tapAddressService.getAddressDescription(address),
      fromFullAddress = address?.toFullAddress(tapAddressService.getAddressDescription(address)) ?: fromCity?.description,
      fromAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }
}

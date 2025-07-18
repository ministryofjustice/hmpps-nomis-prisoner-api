package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository

@Service
@Transactional(readOnly = true)
class MovementsService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  private val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
) {
  fun getTemporaryAbsencesAndMovements(offenderNo: String): OffenderTemporaryAbsencesResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val bookings = offenderBookingRepository.findAllByOffenderNomsId(offenderNo)

    return OffenderTemporaryAbsencesResponse(
      bookings = bookings.map { booking: OffenderBooking ->
        BookingTemporaryAbsences(
          bookingId = booking.bookingId,
          temporaryAbsenceApplications = booking.temporaryAbsenceApplications.map { app: OffenderMovementApplication -> app.toResponse() },
          unscheduledTemporaryAbsences = temporaryAbsenceRepository.findByOffenderBookingAndScheduledTemporaryAbsenceIsNull(booking)
            .map { mov -> mov.toTemporaryAbsenceResponse() },
          unscheduledTemporaryAbsenceReturns = temporaryAbsenceReturnRepository.findByOffenderBookingAndScheduledTemporaryAbsenceIsNull(booking)
            .map { mov -> mov.toTemporaryAbsenceReturnResponse() },
        )
      },
    )
  }

  private fun OffenderMovementApplication.toResponse() = TemporaryAbsenceApplication(
    movementApplicationId = movementApplicationId,
    eventSubType = eventSubType.code,
    applicationDate = applicationDate,
    fromDate = fromDate,
    releaseTime = releaseTime,
    toDate = toDate,
    returnTime = returnTime,
    applicationStatus = applicationStatus.code,
    escortCode = escort?.code,
    transportType = transportType?.code,
    comment = comment,
    // TODO is ID and class enough or do we need the full address here?
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    prisonId = prison?.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    scheduledTemporaryAbsence = scheduledTemporaryAbsence?.toResponse(),
    scheduledTemporaryAbsenceReturn = scheduledTemporaryAbsence?.scheduledTemporaryAbsenceReturn?.toResponse(),
    temporaryAbsence = scheduledTemporaryAbsence?.temporaryAbsence?.toResponse(),
    temporaryAbsenceReturn = scheduledTemporaryAbsence?.scheduledTemporaryAbsenceReturn?.temporaryAbsenceReturn?.toResponse(),
    outsideMovements = outsideMovements.map { it.toResponse() },
    audit = toAudit(),
  )

  private fun OffenderMovementApplicationMulti.toResponse() = TemporaryAbsenceApplicationOutsideMovement(
    outsideMovementId = movementApplicationMultiId,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    eventSubType = eventSubType.code,
    fromDate = fromDate,
    releaseTime = releaseTime,
    toDate = toDate,
    returnTime = returnTime,
    comment = comment,
    toAgencyId = toAgency?.id,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    contactPersonName = contactPersonName,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsence.toResponse() = ScheduledTemporaryAbsence(
    eventId = eventId,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    comment = comment,
    escort = escort.code,
    fromPrison = fromAgency?.id,
    toAgency = toAgency?.id,
    transportType = transportType?.code,
    returnDate = returnDate,
    returnTime = returnTime,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsenceReturn.toResponse() = ScheduledTemporaryAbsenceReturn(
    eventId = eventId,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    comment = comment,
    escort = escort.code,
    fromAgency = fromAgency?.id,
    toPrison = toAgency?.id,
    audit = toAudit(),
  )

  private fun OffenderTemporaryAbsence.toResponse() = TemporaryAbsence(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.code,
    arrestAgency = arrestAgency?.code,
    escort = escort?.code,
    escortText = escortText,
    fromPrison = fromAgency?.id,
    toAgency = toAgency?.id,
    commentText = commentText,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    audit = toAudit(),
  )

  private fun OffenderTemporaryAbsenceReturn.toResponse() = TemporaryAbsenceReturn(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.code,
    escort = escort?.code,
    escortText = escortText,
    fromAgency = fromAgency?.id,
    toPrison = toAgency?.id,
    commentText = commentText,
    fromAddressId = fromAddress?.addressId,
    fromAddressOwnerClass = fromAddress?.addressOwnerClass,
    audit = toAudit(),
  )

  private fun OffenderExternalMovement.toTemporaryAbsenceResponse() = TemporaryAbsence(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.code,
    arrestAgency = arrestAgency?.code,
    escort = escort?.code,
    escortText = escortText,
    fromPrison = fromAgency?.id,
    toAgency = toAgency?.id,
    commentText = commentText,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    audit = toAudit(),
  )

  private fun OffenderExternalMovement.toTemporaryAbsenceReturnResponse() = TemporaryAbsenceReturn(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.code,
    escort = escort?.code,
    escortText = escortText,
    fromAgency = fromAgency?.id,
    toPrison = toAgency?.id,
    commentText = commentText,
    fromAddressId = fromAddress?.addressId,
    fromAddressOwnerClass = fromAddress?.addressOwnerClass,
    audit = toAudit(),
  )
}

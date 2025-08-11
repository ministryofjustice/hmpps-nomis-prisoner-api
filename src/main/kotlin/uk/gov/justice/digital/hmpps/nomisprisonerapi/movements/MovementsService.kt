package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository

@Service
@Transactional(readOnly = true)
class MovementsService(
  private val offenderRepository: OffenderRepository,
  private val offenderMovementApplicationRepository: OffenderMovementApplicationRepository,
  private val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  private val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
) {
  fun getTemporaryAbsencesAndMovements(offenderNo: String): OffenderTemporaryAbsencesResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val movementApplications = offenderMovementApplicationRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .also { it.fixMergedSchedules() }
    val unscheduledTemporaryAbsences = temporaryAbsenceRepository.findAllByOffenderBooking_Offender_NomsIdAndScheduledTemporaryAbsenceIsNull(offenderNo)
    val unscheduledTemporaryAbsenceReturns = temporaryAbsenceReturnRepository.findAllByOffenderBooking_Offender_NomsIdAndScheduledTemporaryAbsenceIsNull(offenderNo)
    val bookingIds = (
      movementApplications.map { it.offenderBooking.bookingId } +
        unscheduledTemporaryAbsences.map { it.offenderBooking.bookingId } +
        unscheduledTemporaryAbsenceReturns.map { it.offenderBooking.bookingId }
      ).toSet()

    return OffenderTemporaryAbsencesResponse(
      bookings = bookingIds.map { bookingId ->
        BookingTemporaryAbsences(
          bookingId = bookingId,
          temporaryAbsenceApplications = movementApplications.filter { it.offenderBooking.bookingId == bookingId }
            .map { it.toResponse() },
          unscheduledTemporaryAbsences = unscheduledTemporaryAbsences.filter { it.offenderBooking.bookingId == bookingId }
            .map { mov -> mov.toTemporaryAbsenceResponse() },
          unscheduledTemporaryAbsenceReturns = unscheduledTemporaryAbsenceReturns.filter { it.offenderBooking.bookingId == bookingId }
            .map { mov -> mov.toTemporaryAbsenceReturnResponse() },
        )
      },
    )
  }

  private fun List<OffenderMovementApplication>.fixMergedSchedules() {
    // find any scheduled returns on the wrong application and remove them (these are created during merges)
    val scheduledReturnsWithWrongParent = this.flatMap { application ->
      application.scheduledTemporaryAbsences.flatMap { it.scheduledTemporaryAbsenceReturns }
        .filter { it.temporaryAbsenceApplication != null && it.temporaryAbsenceApplication != application }
    }

    // detach the scheduled returns with wrong application from their scheduled absence
    this.flatMap { it.scheduledTemporaryAbsences }.forEach {
      it.scheduledTemporaryAbsenceReturns.removeAll(scheduledReturnsWithWrongParent)
    }

    // put the scheduled returns onto the correct application
    scheduledReturnsWithWrongParent.forEach { mergedReturn ->
      this.find { it.movementApplicationId == mergedReturn.temporaryAbsenceApplication?.movementApplicationId }
        ?.scheduledTemporaryAbsences
        ?.first { absence -> absence.scheduledTemporaryAbsenceReturns.isEmpty() }
        ?.scheduledTemporaryAbsenceReturns += mergedReturn
      mergedReturn.temporaryAbsenceApplication = null
    }
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
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    prisonId = prison?.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    absences = scheduledTemporaryAbsences.map {
      Absence(
        scheduledTemporaryAbsence = it.toResponse(),
        scheduledTemporaryAbsenceReturn = it.scheduledTemporaryAbsenceReturns.firstOrNull()?.toResponse(),
        temporaryAbsence = it.temporaryAbsence?.toResponse(),
        temporaryAbsenceReturn = it.scheduledTemporaryAbsenceReturns.firstOrNull()?.temporaryAbsenceReturn?.toResponse(),
      )
    },
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
    escort = escort?.code,
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
    escort = escort?.code,
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

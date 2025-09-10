package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationMultiRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class MovementsService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderMovementApplicationRepository: OffenderMovementApplicationRepository,
  private val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  private val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
  private val scheduledTemporaryAbsenceRepository: OffenderScheduledTemporaryAbsenceRepository,
  private val scheduledTemporaryAbsenceReturnRepository: OffenderScheduledTemporaryAbsenceReturnRepository,
  private val offenderMovementApplicationMultiRepository: OffenderMovementApplicationMultiRepository,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val movementApplicationStatusRepository: ReferenceCodeRepository<MovementApplicationStatus>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val transportTypeRepository: ReferenceCodeRepository<TemporaryAbsenceTransportType>,
  private val addressRepository: AddressRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val movementApplicationTypeRepository: ReferenceCodeRepository<MovementApplicationType>,
  private val temporaryAbsenceTypeRepository: ReferenceCodeRepository<TemporaryAbsenceType>,
  private val temporaryAbsenceSubTypeRepository: ReferenceCodeRepository<TemporaryAbsenceSubType>,
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

    // put the scheduled returns onto the correct application and schedule
    scheduledReturnsWithWrongParent.forEach { mergedReturn ->
      this.find { it.movementApplicationId == mergedReturn.temporaryAbsenceApplication?.movementApplicationId }
        ?.scheduledTemporaryAbsences
        ?.firstOrNull { absence -> absence.scheduledTemporaryAbsenceReturns.isEmpty() && absence.returnDate == mergedReturn.eventDate }
        ?.scheduledTemporaryAbsenceReturns += mergedReturn
      mergedReturn.temporaryAbsenceApplication = null
    }
  }

  fun getTemporaryAbsenceApplication(offenderNo: String, applicationId: Long): TemporaryAbsenceApplicationResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val application = offenderMovementApplicationRepository.findByMovementApplicationIdAndOffenderBooking_Offender_NomsId(applicationId, offenderNo)
      ?: throw NotFoundException("Temporary absence application with id=$applicationId not found for offender with nomsId=$offenderNo")

    return application.toSingleResponse()
  }

  @Transactional
  fun createTemporaryAbsenceApplication(offenderNo: String, request: CreateTemporaryAbsenceApplicationRequest): CreateTemporaryAbsenceApplicationResponse {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Offender $offenderNo not found with a booking")

    val eventSubType = movementReasonRepository.findByIdOrNull(MovementReason.pk(request.eventSubType))
      ?: throw BadDataException("Event sub type $request.eventSubType is invalid")
    val applicationStatus = movementApplicationStatusRepository.findByIdOrNull(MovementApplicationStatus.pk(request.applicationStatus))
      ?: throw BadDataException("Application status ${request.applicationStatus} is invalid")
    val escort = request.escortCode?.let {
      escortRepository.findByIdOrNull(Escort.pk(request.escortCode)) ?: throw BadDataException("Escort code ${request.escortCode} is invalid")
    }
    val transportType = request.transportType?.let {
      transportTypeRepository.findByIdOrNull(TemporaryAbsenceTransportType.pk(request.transportType)) ?: throw BadDataException("Transport type ${request.transportType} is invalid")
    }
    val toAddress = request.toAddressId?.let {
      addressRepository.findByIdOrNull(request.toAddressId) ?: throw BadDataException("Address id ${request.toAddressId} is invalid")
    }
    val prison = request.prisonId?.let {
      agencyLocationRepository.findByIdOrNull(request.prisonId) ?: throw BadDataException("Prison id ${request.prisonId} is invalid")
    }
    val toAgency = request.toAgencyId?.let {
      agencyLocationRepository.findByIdOrNull(request.toAgencyId) ?: throw BadDataException("To agency id ${request.toAgencyId} is invalid")
    }
    val applicationType = movementApplicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(request.applicationType))
      ?: throw BadDataException("Application type ${request.applicationType} is invalid")
    val temporaryAbsenceType = request.temporaryAbsenceType?.let {
      temporaryAbsenceTypeRepository.findByIdOrNull(TemporaryAbsenceType.pk(request.temporaryAbsenceType)) ?: throw BadDataException("Temporary absence type ${request.temporaryAbsenceType} is invalid")
    }
    val temporaryAbsenceSubType = request.temporaryAbsenceSubType?.let {
      temporaryAbsenceSubTypeRepository.findByIdOrNull(TemporaryAbsenceSubType.pk(request.temporaryAbsenceSubType)) ?: throw BadDataException("Temporary absence sub type ${request.temporaryAbsenceSubType} is invalid")
    }

    return OffenderMovementApplication(
      offenderBooking = offenderBooking,
      eventSubType = eventSubType,
      applicationDate = request.applicationDate.atTime(LocalTime.MIDNIGHT),
      applicationTime = request.applicationDate.atTime(LocalTime.MIDNIGHT),
      fromDate = request.fromDate,
      releaseTime = request.releaseTime,
      toDate = request.toDate,
      returnTime = request.returnTime,
      applicationStatus = applicationStatus,
      escort = escort,
      transportType = transportType,
      comment = request.comment,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      toAddress = toAddress,
      prison = prison,
      toAgency = toAgency,
      contactPersonName = request.contactPersonName,
      applicationType = applicationType,
      temporaryAbsenceType = temporaryAbsenceType,
      temporaryAbsenceSubType = temporaryAbsenceSubType,
    ).let {
      offenderMovementApplicationRepository.save(it)
    }.let {
      CreateTemporaryAbsenceApplicationResponse(offenderBooking.bookingId, it.movementApplicationId)
    }
  }

  fun getScheduledTemporaryAbsence(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsence = scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsence.toSingleResponse()
  }

  fun getScheduledTemporaryAbsenceReturn(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceReturnResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsenceReturn = scheduledTemporaryAbsenceReturnRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence return with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsenceReturn.toSingleResponse()
  }

  fun getTemporaryAbsenceApplicationOutsideMovement(offenderNo: String, appMultiId: Long): TemporaryAbsenceApplicationOutsideMovementResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val outsideMovement = offenderMovementApplicationMultiRepository.findByMovementApplicationMultiIdAndOffenderMovementApplication_OffenderBooking_Offender_NomsId(appMultiId, offenderNo)
      ?: throw NotFoundException("Temporary absence application outside movement with id=$appMultiId not found for offender with nomsId=$offenderNo")

    return outsideMovement.toSingleResponse()
  }

  fun getTemporaryAbsence(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsence = temporaryAbsenceRepository.findByOffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    return temporaryAbsence.toSingleResponse()
  }

  fun getTemporaryAbsenceReturn(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceReturnResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsenceReturn = temporaryAbsenceReturnRepository.findByOffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence return with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    return temporaryAbsenceReturn.toSingleResponse()
  }

  private fun OffenderMovementApplication.toResponse() = TemporaryAbsenceApplication(
    movementApplicationId = movementApplicationId,
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

  private fun OffenderMovementApplication.toSingleResponse() = TemporaryAbsenceApplicationResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = movementApplicationId,
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
    prisonId = prison?.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsence.toSingleResponse() = ScheduledTemporaryAbsenceResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = temporaryAbsenceApplication.movementApplicationId,
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

  private fun OffenderScheduledTemporaryAbsenceReturn.toSingleResponse() = ScheduledTemporaryAbsenceReturnResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = scheduledTemporaryAbsence.temporaryAbsenceApplication.movementApplicationId,
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

  private fun OffenderMovementApplicationMulti.toSingleResponse() = TemporaryAbsenceApplicationOutsideMovementResponse(
    bookingId = offenderMovementApplication.offenderBooking.bookingId,
    movementApplicationId = offenderMovementApplication.movementApplicationId,
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

  private fun OffenderTemporaryAbsence.toSingleResponse() = TemporaryAbsenceResponse(
    bookingId = id.offenderBooking.bookingId,
    sequence = id.sequence,
    scheduledTemporaryAbsenceId = scheduledTemporaryAbsence?.eventId,
    movementApplicationId = scheduledTemporaryAbsence?.temporaryAbsenceApplication?.movementApplicationId,
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

  private fun OffenderTemporaryAbsenceReturn.toSingleResponse() = TemporaryAbsenceReturnResponse(
    bookingId = id.offenderBooking.bookingId,
    sequence = id.sequence,
    scheduledTemporaryAbsenceReturnId = scheduledTemporaryAbsenceReturn?.eventId,
    movementApplicationId = scheduledTemporaryAbsence?.temporaryAbsenceApplication?.movementApplicationId,
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

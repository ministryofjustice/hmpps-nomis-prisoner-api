package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ArrestAgency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.activeExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.maxMovementSequence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
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
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val arrestAgencyRepository: ReferenceCodeRepository<ArrestAgency>,
  private val corporateAddressRepository: CorporateAddressRepository,
  private val agencyLocationAddressRepository: AgencyLocationAddressRepository,
  movementTypeRepository: ReferenceCodeRepository<MovementType>,
) {

  private val tapMovementType = movementTypeRepository.findByIdOrNull(MovementType.pk("TAP"))
    ?: throw IllegalStateException("TAP movement type not found")

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
  fun upsertTemporaryAbsenceApplication(offenderNo: String, request: UpsertTemporaryAbsenceApplicationRequest): UpsertTemporaryAbsenceApplicationResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val eventSubType = movementReasonOrThrow(request.eventSubType)
    val applicationStatus = applicationStatusOrThrow(request.applicationStatus)
    val escort = request.escortCode?.let { escortOrThrow(request.escortCode) }
    val transportType = request.transportType?.let { transportTypeOrThrow(request.transportType) }
    val prison = agencyLocationOrThrow(request.prisonId)
    val applicationType = movementApplicationTypeOrThrow(request.applicationType)
    val temporaryAbsenceType = request.temporaryAbsenceType?.let { temporaryAbsenceTypeOrThrow(request.temporaryAbsenceType) }
    val temporaryAbsenceSubType = request.temporaryAbsenceSubType?.let { temporaryAbsenceSubTypeOrThrow(request.temporaryAbsenceSubType) }

    val application = request.movementApplicationId
      ?.let {
        offenderMovementApplicationRepository.findByIdOrNull(request.movementApplicationId)
          ?: throw NotFoundException("Temporary absence application with id=$request.movementApplicationId not found for offender with nomsId=$offenderNo")
      } ?: OffenderMovementApplication(
      offenderBooking = offenderBooking,
      applicationDate = request.applicationDate.atTime(LocalTime.MIDNIGHT),
      applicationTime = request.applicationDate.atTime(LocalTime.MIDNIGHT),
      prison = prison,
      applicationType = applicationType,
      eventSubType = eventSubType,
      fromDate = request.fromDate,
      releaseTime = request.releaseTime,
      toDate = request.toDate,
      returnTime = request.returnTime,
      applicationStatus = applicationStatus,
    )

    with(application) {
      this.fromDate = request.fromDate
      this.releaseTime = request.releaseTime
      this.toDate = request.toDate
      this.returnTime = request.returnTime
      this.applicationStatus = applicationStatus
      this.transportType = transportType
      this.escort = escort
      this.comment = request.comment
      this.toAddressOwnerClass = this.scheduledTemporaryAbsences.firstOrNull()?.toAddressOwnerClass
      this.toAddress = this.scheduledTemporaryAbsences.firstOrNull()?.toAddress
      this.toAgency = this.scheduledTemporaryAbsences.firstOrNull()?.toAgency
      this.contactPersonName = request.contactPersonName
      this.applicationType = applicationType
      this.temporaryAbsenceType = temporaryAbsenceType
      this.temporaryAbsenceSubType = temporaryAbsenceSubType
    }

    return offenderMovementApplicationRepository.save(application)
      .let { UpsertTemporaryAbsenceApplicationResponse(offenderBooking.bookingId, it.movementApplicationId) }
  }

  fun getScheduledTemporaryAbsence(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsence = scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsence.toSingleResponse()
  }

  @Transactional
  fun createScheduledTemporaryAbsence(offenderNo: String, request: CreateScheduledTemporaryAbsenceRequest): CreateScheduledTemporaryAbsenceResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val application = movementApplicationOrThrow(request.movementApplicationId)
    val eventSubType = movementReasonOrThrow(request.eventSubType)
    val eventStatus = eventStatusOrThrow(request.eventStatus)
    val escort = escortOrThrow(request.escort)
    val fromPrison = agencyLocationOrThrow(request.fromPrison)
    val toAgency = request.toAgency?.let { agencyLocationOrThrow(request.toAgency) }
    val transportType = request.transportType?.let { transportTypeOrThrow(request.transportType) }
    val toAddress = request.toAddressId?.let { addressOrThrow(request.toAddressId) }

    return OffenderScheduledTemporaryAbsence(
      offenderBooking = offenderBooking,
      eventDate = request.eventDate,
      startTime = request.startTime,
      eventSubType = eventSubType,
      eventStatus = eventStatus,
      comment = request.comment,
      escort = escort,
      fromPrison = fromPrison,
      toAgency = toAgency,
      transportType = transportType,
      returnDate = request.returnDate,
      returnTime = request.returnTime,
      temporaryAbsenceApplication = application,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      toAddress = toAddress,
      applicationDate = request.applicationDate,
      applicationTime = request.applicationTime,
    ).let {
      scheduledTemporaryAbsenceRepository.save(it)
    }.let {
      CreateScheduledTemporaryAbsenceResponse(
        bookingId = offenderBooking.bookingId,
        movementApplicationId = application.movementApplicationId,
        eventId = it.eventId,
      )
    }
  }

  fun getScheduledTemporaryAbsenceReturn(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceReturnResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsenceReturn = scheduledTemporaryAbsenceReturnRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence return with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsenceReturn.toSingleResponse()
  }

  @Transactional
  fun createScheduledTemporaryAbsenceReturn(offenderNo: String, request: CreateScheduledTemporaryAbsenceReturnRequest): CreateScheduledTemporaryAbsenceReturnResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val application = movementApplicationOrThrow(request.movementApplicationId)
    val scheduledTemporaryAbsence = scheduledTemporaryAbsenceOrThrow(request.scheduledTemporaryAbsenceEventId)
    val eventSubType = movementReasonOrThrow(request.eventSubType)
    val eventStatus = eventStatusOrThrow(request.eventStatus)
    val escort = escortOrThrow(request.escort)
    val fromAgency = request.fromAgency?.let { agencyLocationOrThrow(request.fromAgency) }
    val toPrison = request.toPrison?.let { agencyLocationOrThrow(request.toPrison) }

    return OffenderScheduledTemporaryAbsenceReturn(
      offenderBooking = offenderBooking,
      eventDate = request.eventDate,
      startTime = request.startTime,
      eventSubType = eventSubType,
      eventStatus = eventStatus,
      comment = request.comment,
      escort = escort,
      fromAgency = fromAgency,
      toPrison = toPrison,
      temporaryAbsenceApplication = application,
      scheduledTemporaryAbsence = scheduledTemporaryAbsence,
    ).let {
      scheduledTemporaryAbsenceReturnRepository.save(it)
    }.let {
      CreateScheduledTemporaryAbsenceReturnResponse(
        bookingId = offenderBooking.bookingId,
        movementApplicationId = application.movementApplicationId,
        eventId = it.eventId,
      )
    }
  }

  fun getTemporaryAbsenceApplicationOutsideMovement(offenderNo: String, appMultiId: Long): TemporaryAbsenceApplicationOutsideMovementResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val outsideMovement = offenderMovementApplicationMultiRepository.findByMovementApplicationMultiIdAndOffenderMovementApplication_OffenderBooking_Offender_NomsId(appMultiId, offenderNo)
      ?: throw NotFoundException("Temporary absence application outside movement with id=$appMultiId not found for offender with nomsId=$offenderNo")

    return outsideMovement.toSingleResponse()
  }

  @Transactional
  fun createTemporaryAbsenceOutsideMovement(offenderNo: String, request: CreateTemporaryAbsenceOutsideMovementRequest): CreateTemporaryAbsenceOutsideMovementResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val application = movementApplicationOrThrow(request.movementApplicationId)
    val eventSubType = movementReasonOrThrow(request.eventSubType)
    val toAddress = request.toAddressId?.let { addressOrThrow(request.toAddressId) }
    val toAgency = request.toAgencyId?.let { agencyLocationOrThrow(request.toAgencyId) }
    val temporaryAbsenceType = request.temporaryAbsenceType?.let { temporaryAbsenceTypeOrThrow(request.temporaryAbsenceType) }
    val temporaryAbsenceSubType = request.temporaryAbsenceSubType?.let { temporaryAbsenceSubTypeOrThrow(request.temporaryAbsenceSubType) }

    return OffenderMovementApplicationMulti(
      offenderMovementApplication = application,
      eventSubType = eventSubType,
      fromDate = request.fromDate,
      releaseTime = request.releaseTime,
      toDate = request.toDate,
      returnTime = request.returnTime,
      comment = request.comment,
      toAgency = toAgency,
      toAddress = toAddress,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      contactPersonName = request.contactPersonName,
      temporaryAbsenceType = temporaryAbsenceType,
      temporaryAbsenceSubType = temporaryAbsenceSubType,
    ).let {
      offenderMovementApplicationMultiRepository.save(it)
    }.let {
      CreateTemporaryAbsenceOutsideMovementResponse(
        bookingId = offenderBooking.bookingId,
        movementApplicationId = application.movementApplicationId,
        outsideMovementId = it.movementApplicationMultiId,
      )
    }
  }

  fun getTemporaryAbsence(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsence = temporaryAbsenceRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    return temporaryAbsence.toSingleResponse()
  }

  @Transactional
  fun createTemporaryAbsence(offenderNo: String, request: CreateTemporaryAbsenceRequest): CreateTemporaryAbsenceResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val scheduledTemporaryAbsence = request.scheduledTemporaryAbsenceId?.let { scheduledTemporaryAbsenceOrThrow(request.scheduledTemporaryAbsenceId) }
    val movementReason = movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { escortOrThrow(request.escort) }
    val fromPrison = request.fromPrison?.let { agencyLocationOrThrow(request.fromPrison) }
    val toAgency = request.toAgency?.let { agencyLocationOrThrow(request.toAgency) }
    val toAddress = request.toAddressId?.let { addressOrThrow(request.toAddressId) }

    return OffenderTemporaryAbsence(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      scheduledTemporaryAbsence = scheduledTemporaryAbsence,
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
      temporaryAbsenceRepository.save(it)
    }.let {
      CreateTemporaryAbsenceResponse(
        bookingId = offenderBooking.bookingId,
        movementSequence = it.id.sequence,
      )
    }
  }

  fun getTemporaryAbsenceReturn(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceReturnResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsenceReturn = temporaryAbsenceReturnRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence return with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    return temporaryAbsenceReturn.toSingleResponse()
  }

  @Transactional
  fun createTemporaryAbsenceReturn(offenderNo: String, request: CreateTemporaryAbsenceReturnRequest): CreateTemporaryAbsenceReturnResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val scheduledTemporaryAbsenceReturn = request.scheduledTemporaryAbsenceReturnId?.let { scheduledTemporaryAbsenceReturnOrThrow(request.scheduledTemporaryAbsenceReturnId) }
    val movementReason = movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { escortOrThrow(request.escort) }
    val fromAgency = request.fromAgency?.let { agencyLocationOrThrow(request.fromAgency) }
    val toPrison = request.toPrison?.let { agencyLocationOrThrow(request.toPrison) }
    val fromAddress = request.fromAddressId?.let { addressOrThrow(request.fromAddressId) }

    return OffenderTemporaryAbsenceReturn(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      scheduledTemporaryAbsenceReturn = scheduledTemporaryAbsenceReturn,
      scheduledTemporaryAbsence = scheduledTemporaryAbsenceReturn?.scheduledTemporaryAbsence,
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
      temporaryAbsenceReturnRepository.save(it)
    }.let {
      CreateTemporaryAbsenceReturnResponse(
        bookingId = offenderBooking.bookingId,
        movementSequence = it.id.sequence,
      )
    }
  }

  private fun offenderBookingOrThrow(offenderNo: String) = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("Offender $offenderNo not found with a booking")

  private fun movementApplicationOrThrow(movementApplicationId: Long) = offenderMovementApplicationRepository.findByIdOrNull(movementApplicationId)
    ?: throw BadDataException("Movement application $movementApplicationId does not exist")

  private fun scheduledTemporaryAbsenceOrThrow(eventId: Long) = scheduledTemporaryAbsenceRepository.findByIdOrNull(eventId)
    ?: throw BadDataException("Scheduled temporary absence $eventId does not exist")

  private fun scheduledTemporaryAbsenceReturnOrThrow(eventId: Long) = scheduledTemporaryAbsenceReturnRepository.findByIdOrNull(eventId)
    ?: throw BadDataException("Scheduled temporary absence return $eventId does not exist")

  private fun movementReasonOrThrow(movementReason: String) = movementReasonRepository.findByIdOrNull(MovementReason.pk(movementReason))
    ?: throw BadDataException("Event sub type $movementReason is invalid")

  private fun applicationStatusOrThrow(applicationStatus: String) = movementApplicationStatusRepository.findByIdOrNull(MovementApplicationStatus.pk(applicationStatus))
    ?: throw BadDataException("Application status $applicationStatus is invalid")

  private fun escortOrThrow(escort: String) = escortRepository.findByIdOrNull(Escort.pk(escort))
    ?: throw BadDataException("Escort code $escort is invalid")

  private fun transportTypeOrThrow(transportType: String) = transportTypeRepository.findByIdOrNull(TemporaryAbsenceTransportType.pk(transportType))
    ?: throw BadDataException("Transport type $transportType is invalid")

  private fun addressOrThrow(addressId: Long) = addressRepository.findByIdOrNull(addressId)
    ?: throw BadDataException("Address id $addressId is invalid")

  private fun agencyLocationOrThrow(agencyId: String) = agencyLocationRepository.findByIdOrNull(agencyId)
    ?: throw BadDataException("Agency id $agencyId is invalid")

  private fun movementApplicationTypeOrThrow(applicationType: String) = movementApplicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(applicationType))
    ?: throw BadDataException("Application type $applicationType is invalid")

  private fun temporaryAbsenceTypeOrThrow(temporaryAbsenceType: String) = temporaryAbsenceTypeRepository.findByIdOrNull(TemporaryAbsenceType.pk(temporaryAbsenceType))
    ?: throw BadDataException("Temporary absence type $temporaryAbsenceType is invalid")

  private fun temporaryAbsenceSubTypeOrThrow(temporaryAbsenceSubType: String) = temporaryAbsenceSubTypeRepository.findByIdOrNull(TemporaryAbsenceSubType.pk(temporaryAbsenceSubType))
    ?: throw BadDataException("Temporary absence sub type $temporaryAbsenceSubType is invalid")

  private fun eventStatusOrThrow(eventStatus: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(eventStatus))
    ?: throw BadDataException("Event status $eventStatus is invalid")

  private fun arrestAgencyOrThrow(arrestAgency: String) = arrestAgencyRepository.findByIdOrNull(ArrestAgency.pk(arrestAgency))
    ?: throw BadDataException("Arrest Agency $arrestAgency is invalid")

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
    prisonId = prison.id,
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
    eventDate = eventDate ?: temporaryAbsenceApplication.fromDate,
    startTime = startTime ?: temporaryAbsenceApplication.releaseTime,
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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddressView?.fullAddress?.trim(),
    toAddressPostcode = toAddress?.postalCode,
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsenceReturn.toResponse() = ScheduledTemporaryAbsenceReturn(
    eventId = eventId,
    eventDate = eventDate ?: scheduledTemporaryAbsence.temporaryAbsenceApplication.fromDate,
    startTime = startTime ?: scheduledTemporaryAbsence.temporaryAbsenceApplication.releaseTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    comment = comment,
    escort = escort?.code,
    fromAgency = fromAgency?.id,
    toPrison = toAgency?.id,
    audit = toAudit(),
  )

  private fun OffenderTemporaryAbsence.toResponse(): TemporaryAbsence {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress ?: scheduledTemporaryAbsence?.toAddress
    val toAddressView = toAddressView ?: scheduledTemporaryAbsence?.toAddressView
    return TemporaryAbsence(
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
      toAddressDescription = getAddressDescription(toAddress),
      toFullAddress = toAddressView?.fullAddress?.trim(),
      toAddressPostcode = toAddress?.postalCode,
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsenceReturn.toResponse(): TemporaryAbsenceReturn {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddress
      ?: scheduledTemporaryAbsence?.toAddress
    val addressView = fromAddressView
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddressView
      ?: scheduledTemporaryAbsence?.toAddressView
    return TemporaryAbsenceReturn(
      sequence = id.sequence,
      movementDate = movementDate,
      movementTime = movementTime,
      movementReason = movementReason.code,
      escort = escort?.code,
      escortText = escortText,
      fromAgency = fromAgency?.id,
      toPrison = toAgency?.id,
      commentText = commentText,
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = getAddressDescription(address),
      fromFullAddress = addressView?.fullAddress?.trim(),
      fromAddressPostcode = address?.postalCode,
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsence.toTemporaryAbsenceResponse() = TemporaryAbsence(
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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddressView?.fullAddress?.trim() ?: toCity?.description,
    toAddressPostcode = toAddress?.postalCode,
    audit = toAudit(),
  )

  private fun OffenderTemporaryAbsenceReturn.toTemporaryAbsenceReturnResponse() = TemporaryAbsenceReturn(
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
    fromAddressDescription = getAddressDescription(fromAddress),
    fromFullAddress = fromAddressView?.fullAddress?.trim() ?: fromCity?.description,
    fromAddressPostcode = fromAddress?.postalCode,
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
    prisonId = prison.id,
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
    eventDate = eventDate ?: temporaryAbsenceApplication.fromDate,
    startTime = startTime ?: temporaryAbsenceApplication.releaseTime,
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    inboundEventStatus = scheduledTemporaryAbsenceReturns.firstOrNull()?.eventStatus?.code,
    comment = comment,
    escort = escort?.code,
    fromPrison = fromAgency?.id,
    toAgency = toAgency?.id,
    transportType = transportType?.code,
    returnDate = returnDate,
    returnTime = returnTime,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddressView?.fullAddress,
    toAddressPostcode = toAddress?.postalCode,
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    contactPersonName = contactPersonName,
    temporaryAbsenceType = temporaryAbsenceApplication.temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceApplication.temporaryAbsenceSubType?.code,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsenceReturn.toSingleResponse() = ScheduledTemporaryAbsenceReturnResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = scheduledTemporaryAbsence.temporaryAbsenceApplication.movementApplicationId,
    eventId = eventId,
    eventDate = eventDate ?: scheduledTemporaryAbsence.temporaryAbsenceApplication.fromDate,
    startTime = startTime ?: scheduledTemporaryAbsence.temporaryAbsenceApplication.releaseTime,
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

  private fun OffenderTemporaryAbsence.toSingleResponse(): TemporaryAbsenceResponse {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress ?: scheduledTemporaryAbsence?.toAddress
    val toAddressView = toAddressView ?: scheduledTemporaryAbsence?.toAddressView
    return TemporaryAbsenceResponse(
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
      toAddressDescription = getAddressDescription(toAddress),
      toFullAddress = toAddressView?.fullAddress ?: toCity?.description,
      toAddressPostcode = toAddress?.postalCode,
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsenceReturn.toSingleResponse(): TemporaryAbsenceReturnResponse {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddress
      ?: scheduledTemporaryAbsence?.toAddress
    val addressView = fromAddressView
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddressView
      ?: scheduledTemporaryAbsence?.toAddressView
    return TemporaryAbsenceReturnResponse(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      scheduledTemporaryAbsenceId = scheduledTemporaryAbsence?.eventId,
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
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = getAddressDescription(address),
      fromFullAddress = addressView?.fullAddress ?: fromCity?.description,
      fromAddressPostcode = address?.postalCode,
      audit = toAudit(),
    )
  }

  private fun getAddressDescription(address: Address?) = address?.addressId?.let {
    when (address.addressOwnerClass) {
      "CORP" -> corporateAddressRepository.findByIdOrNull(it)?.corporate?.corporateName
      "AGY" -> agencyLocationAddressRepository.findByIdOrNull(it)?.agencyLocation?.description
      else -> null
    }
  }
}

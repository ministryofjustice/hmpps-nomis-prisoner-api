package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ArrestAgency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.CorporateInsertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.TapAddressInsertRepository
import java.time.LocalDateTime
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
  private val offenderAddressRepository: OffenderAddressRepository,
  private val corporateRepository: CorporateRepository,
  private val corporateInsertRepository: CorporateInsertRepository,
  private val tapAddressInsertRepository: TapAddressInsertRepository,
  private val externalMovementsRepository: OffenderExternalMovementRepository,
  addressUsageTypeRepository: ReferenceCodeRepository<AddressUsageType>,
  movementTypeRepository: ReferenceCodeRepository<MovementType>,
) {
  companion object {
    val MAX_TAP_COMMENT_LENGTH = 225
  }

  private val tapMovementType = movementTypeRepository.findByIdOrNull(MovementType.pk("TAP"))
    ?: throw IllegalStateException("TAP movement type not found")

  private val rotlAddressType = addressUsageTypeRepository.findByIdOrNull(AddressUsageType.pk("ROTL"))

  fun getTemporaryAbsencesAndMovements(offenderNo: String): OffenderTemporaryAbsencesResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    // We need to run these queries before findAllByOffenderBooking_Offender_NomsId otherwise if Hibernate finds an entity of
    // type OffenderTemporaryAbsence in the session where it's expecting an OffenderTemporaryAbsenceReturn it tries (and fails)
    // to use the wrong type instead of respecting the @NotFound(IGNORE). So we run these queries before the entity is loaded
    // into the session and the @NotFound is respected.
    val allTemporaryAbsences = temporaryAbsenceRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    val allTemporaryAbsenceReturns = temporaryAbsenceReturnRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)

    val movementApplications = offenderMovementApplicationRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .also { it.fixMergedSchedules() }
      .also { it.unlinkCorruptTemporaryAbsenceReturns() }

    // To find the unscheduled movements we get all movements and remove those linked to a TAP application. This is necessary because of corrupt movements linked to the wrong application.
    val unscheduledTemporaryAbsences = allTemporaryAbsences - movementApplications.temporaryAbsences()
    val unscheduledTemporaryAbsenceReturns = allTemporaryAbsenceReturns - movementApplications.temporaryAbsenceReturns()

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

  /*
   * Cut links to any return movements that have ended up on the wrong application due to corrupt data
   */
  private fun List<OffenderMovementApplication>.unlinkCorruptTemporaryAbsenceReturns() {
    forEach {
      it.scheduledTemporaryAbsences.forEach {
        it.scheduledTemporaryAbsenceReturns.forEach { scheduledReturn ->
          scheduledReturn.temporaryAbsenceReturn?.also { returnMovement ->
            if (returnMovement.unlinkWrongSchedules()) {
              scheduledReturn.temporaryAbsenceReturn = null
            }
          }
        }
      }
    }
  }

  private fun List<OffenderMovementApplication>.temporaryAbsences() = flatMap {
    it.scheduledTemporaryAbsences.mapNotNull {
      it.temporaryAbsence
    }
  }.toSet()

  private fun List<OffenderMovementApplication>.temporaryAbsenceReturns() = flatMap {
    it.scheduledTemporaryAbsences.flatMap {
      it.scheduledTemporaryAbsenceReturns.mapNotNull {
        it.temporaryAbsenceReturn
      }
    }
  }.toSet()

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
    // Make sure all requested addresses exist, but just take the first one as NOMIS only supports a single address at the application level
    val toAddress = request.toAddresses
      .map { findOrCreateAddress(it, offenderBooking.offender) }
      .firstOrNull()
    val toAddressAgency = toAddress
      .takeIf { it?.addressOwnerClass == "AGY" }
      ?.let { it as AgencyLocationAddress }
      ?.agencyLocation

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
      toAgency = toAddressAgency,
      toAddress = toAddress,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
    )

    with(application) {
      this.fromDate = request.fromDate
      this.releaseTime = request.releaseTime
      this.toDate = request.toDate
      this.returnTime = request.returnTime
      this.applicationStatus = applicationStatus
      this.transportType = transportType
      this.escort = escort
      this.comment = request.comment?.truncateToUtf8Length(MAX_TAP_COMMENT_LENGTH, includeSeeDpsSuffix = true)
      this.contactPersonName = request.contactPersonName
      this.applicationType = applicationType
      this.temporaryAbsenceType = temporaryAbsenceType
      this.temporaryAbsenceSubType = temporaryAbsenceSubType
      this.eventSubType = eventSubType
      this.toAgency = toAddressAgency ?: this.toAgency
      this.toAddress = toAddress ?: this.toAddress
      this.toAddressOwnerClass = toAddress?.addressOwnerClass ?: this.toAddressOwnerClass
    }

    return offenderMovementApplicationRepository.save(application)
      .let { UpsertTemporaryAbsenceApplicationResponse(offenderBooking.bookingId, it.movementApplicationId) }
  }

  @Transactional
  fun deleteTemporaryAbsenceApplication(offenderNo: String, applicationId: Long) {
    offenderMovementApplicationRepository.findByIdOrNull(applicationId)
      ?.also { application ->
        if (application.scheduledTemporaryAbsences.isNotEmpty()) {
          throw ConflictException("Cannot delete temporary absence applicationId $applicationId because it has scheduled temporary absences")
        }

        offenderBookingRepository.findByIdOrNull(application.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("ApplicationId $applicationId exists on a different offender") }

        offenderMovementApplicationRepository.delete(application)
      }
  }

  fun getScheduledTemporaryAbsence(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsence = scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsence.toSingleResponse(scheduledAbsence.temporaryAbsenceApplication.returnTime)
  }

  @Transactional
  fun upsertScheduledTemporaryAbsence(offenderNo: String, request: UpsertScheduledTemporaryAbsenceRequest): UpsertScheduledTemporaryAbsenceResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val application = movementApplicationOrThrow(request.movementApplicationId)
    val eventSubType = movementReasonOrThrow(request.eventSubType)
    val eventStatus = eventStatusOrThrow(request.eventStatus)
    val escort = request.escort?.let { escortOrThrow(request.escort) }
    val fromPrison = agencyLocationOrThrow(request.fromPrison)
    val toAgency = request.toAgency?.let { agencyLocationOrThrow(request.toAgency) }
    val transportType = request.transportType?.let { transportTypeOrThrow(request.transportType) }
    val toAddress = findAddressOrThrow(request.toAddress, offenderBooking.offender)

    val schedule = request.eventId
      ?.let { scheduledTemporaryAbsenceRepository.findById(request.eventId).get() }
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
      ?: OffenderScheduledTemporaryAbsence(
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
        temporaryAbsenceApplication = application,
        toAddressOwnerClass = toAddress.addressOwnerClass,
        toAddress = toAddress,
        applicationDate = request.applicationDate,
        applicationTime = request.applicationTime,
      )

    val scheduledReturn = schedule.scheduledTemporaryAbsenceReturns.firstOrNull()
      ?.apply {
        // TODO As we don't create the scheduled return until the outbound movement has happened we should never need to delete the scheduled return. I think. But if we did then we'd set it to null here. Review this.
        this.eventStatus = eventStatusOrThrow(request.returnEventStatus ?: "SCH")
        this.eventDate = request.returnDate
        this.startTime = request.returnTime
        this.eventSubType = eventSubType
        this.escort = escort
        this.fromAgency = toAgency
        this.toAgency = fromPrison
      }
      ?: let {
        if (request.eventStatus == "COMP") {
          OffenderScheduledTemporaryAbsenceReturn(
            offenderBooking = offenderBooking,
            temporaryAbsenceApplication = application,
            scheduledTemporaryAbsence = schedule,
            eventStatus = eventStatusOrThrow(request.returnEventStatus ?: "SCH"),
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

    schedule.scheduledTemporaryAbsenceReturns = scheduledReturn?.let { mutableListOf(scheduledReturn) } ?: mutableListOf()

    scheduledTemporaryAbsenceRepository.save(schedule)

    return UpsertScheduledTemporaryAbsenceResponse(
      bookingId = offenderBooking.bookingId,
      movementApplicationId = application.movementApplicationId,
      eventId = schedule.eventId,
      addressId = toAddress.addressId,
      addressOwnerClass = toAddress.addressOwnerClass,
    )
  }

  fun getScheduledTemporaryAbsenceReturn(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceReturnResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsenceReturn = scheduledTemporaryAbsenceReturnRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence return with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsenceReturn.toSingleResponse()
  }

  fun getTemporaryAbsence(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsence = temporaryAbsenceRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    // Despite being non-nullable, Hibernate happily loads a null application if missing in NOMIS - if so then make the movement unscheduled
    if (temporaryAbsence.scheduledTemporaryAbsence?.temporaryAbsenceApplication == null) {
      temporaryAbsence.scheduledTemporaryAbsence = null
    }

    return temporaryAbsence.toSingleResponse()
  }

  @Transactional
  fun createTemporaryAbsence(offenderNo: String, request: CreateTemporaryAbsenceRequest): CreateTemporaryAbsenceResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val scheduledTemporaryAbsence = request.scheduledTemporaryAbsenceId?.let { scheduledTemporaryAbsenceOrThrow(request.scheduledTemporaryAbsenceId) }
    val movementReason = movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { escortOrThrow(request.escort) }
    val fromPrison = agencyLocationOrThrow(request.fromPrison)
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

    // Despite being non-nullable, Hibernate happily loads a null application if missing in NOMIS - if so then make the movement unscheduled
    if (temporaryAbsenceReturn.scheduledTemporaryAbsence?.temporaryAbsenceApplication == null) {
      temporaryAbsenceReturn.scheduledTemporaryAbsence = null
      temporaryAbsenceReturn.scheduledTemporaryAbsenceReturn = null
    }

    temporaryAbsenceReturn.unlinkWrongSchedules()
    return temporaryAbsenceReturn.toSingleResponse()
  }

  // Make a temporary absence return unscheduled if the schedules have been corrupted
  private fun OffenderTemporaryAbsenceReturn.unlinkWrongSchedules(): Boolean = when (scheduledTemporaryAbsenceReturn) {
    null -> false
    in scheduledTemporaryAbsence?.scheduledTemporaryAbsenceReturns ?: emptyList() -> false
    else -> {
      scheduledTemporaryAbsence = null
      scheduledTemporaryAbsenceReturn = null
      true
    }
  }

  @Transactional
  fun createTemporaryAbsenceReturn(offenderNo: String, request: CreateTemporaryAbsenceReturnRequest): CreateTemporaryAbsenceReturnResponse {
    val offenderBooking = offenderBookingOrThrow(offenderNo)
    val scheduledTemporaryAbsenceReturn = request.scheduledTemporaryAbsenceReturnId?.let { scheduledTemporaryAbsenceReturnOrThrow(request.scheduledTemporaryAbsenceReturnId) }
    val movementReason = movementReasonOrThrow(request.movementReason)
    val arrestAgency = request.arrestAgency?.let { arrestAgencyOrThrow(request.arrestAgency) }
    val escort = request.escort?.let { escortOrThrow(request.escort) }
    val fromAgency = request.fromAgency?.let { agencyLocationOrThrow(request.fromAgency) }
    val toPrison = agencyLocationOrThrow(request.toPrison)
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

  fun getTemporaryAbsencesSummary(offenderNo: String): OffenderTemporaryAbsenceSummaryResponse {
    offenderOrThrow(offenderNo)
    val scheduledIn = externalMovementsRepository.countOffenderScheduledIn(offenderNo)
    val scheduledOut = externalMovementsRepository.countOffenderScheduledOut(offenderNo)
    val unscheduledIn = externalMovementsRepository.countOffenderUnscheduledIn(offenderNo)
    val unscheduledOut = externalMovementsRepository.countOffenderUnscheduledOut(offenderNo)
    return OffenderTemporaryAbsenceSummaryResponse(
      applications = Applications(count = offenderMovementApplicationRepository.countByOffenderBooking_Offender_NomsId(offenderNo)),
      scheduledOutMovements = ScheduledOut(count = scheduledTemporaryAbsenceRepository.countByOffenderBooking_Offender_NomsId_AndTemporaryAbsenceApplicationIsNotNull(offenderNo)),
      movements = Movements(
        count = scheduledIn + scheduledOut + unscheduledIn + unscheduledOut,
        scheduled = MovementsByDirection(outCount = scheduledOut, inCount = scheduledIn),
        unscheduled = MovementsByDirection(outCount = unscheduledOut, inCount = unscheduledIn),
      ),
    )
  }

  fun getTemporaryAbsencesAndMovementIds(offenderNo: String): OffenderTemporaryAbsenceIdsResponse = getTemporaryAbsencesAndMovements(offenderNo).let {
    OffenderTemporaryAbsenceIdsResponse(
      applicationIds = it.bookings.flatMap { it.temporaryAbsenceApplications.map { it.movementApplicationId } },
      scheduleIds = it.bookings.flatMap {
        it.temporaryAbsenceApplications.flatMap {
          it.absences.flatMap {
            listOfNotNull(
              it.scheduledTemporaryAbsence?.eventId,
              it.scheduledTemporaryAbsenceReturn?.eventId,
            )
          }
        }
      },
      scheduledMovementOutIds = it.bookings.flatMap { booking ->
        booking.temporaryAbsenceApplications.flatMap {
          it.absences.flatMap {
            listOfNotNull(
              it.temporaryAbsence?.let { OffenderTemporaryAbsenceId(booking.bookingId, it.sequence) },
            )
          }
        }
      },
      scheduledMovementInIds = it.bookings.flatMap { booking ->
        booking.temporaryAbsenceApplications.flatMap {
          it.absences.flatMap {
            listOfNotNull(
              it.temporaryAbsenceReturn?.let { OffenderTemporaryAbsenceId(booking.bookingId, it.sequence) },
            )
          }
        }
      },
      unscheduledMovementOutIds = it.bookings.flatMap { booking ->
        booking.unscheduledTemporaryAbsences.map {
          OffenderTemporaryAbsenceId(
            booking.bookingId,
            it.sequence,
          )
        }
      },
      unscheduledMovementInIds = it.bookings.flatMap { booking ->
        booking.unscheduledTemporaryAbsenceReturns.map {
          OffenderTemporaryAbsenceId(
            booking.bookingId,
            it.sequence,
          )
        }
      },
    )
  }

  private fun offenderOrThrow(offenderNo: String) = offenderRepository.findRootByNomsId(offenderNo)
    ?: throw NotFoundException("Offender $offenderNo not found")

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

  private fun findOrCreateAddress(request: UpsertTemporaryAbsenceAddress, offender: Offender): Address {
    // If we have an address id then use that
    if (request.id != null) return addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    return when (request.name) {
      null -> findOrCreateOffenderAddress(request.addressText, request.postalCode, offender)
      else -> findOrCreateCorporateAddress(request.name, request.addressText, request.postalCode)
    }
  }

  private fun formatAddressText(addressText: String): Pair<String, String?> {
    val maxPremiseLength = 135

    if (addressText.length <= maxPremiseLength) {
      return addressText.trim() to null
    }

    val split = if (addressText.substring(maxPremiseLength, maxPremiseLength + 1) == " ") {
      maxPremiseLength
    } else {
      addressText.substring(0, maxPremiseLength).lastIndexOf(" ")
    }
    return addressText.substring(0, split).trim() to addressText.substring(split, addressText.length).trim()
  }

  private fun findOrCreateOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress {
    val (premise, street) = formatAddressText(addressText)
    tapAddressInsertRepository.insertAddressIfNotExists("OFF", offender.rootOffenderId!!, premise, street, postalCode)

    val address = offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)
      .first { (it.premise == premise && it.street == street && it.postalCode == postalCode) }
    if (address.usages.none { it.addressUsage == rotlAddressType }) {
      address.apply {
        usages += AddressUsage(AddressUsageId(this, "ROTL"), true, rotlAddressType)
      }
    }

    return address
  }

  private fun findOrCreateCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress {
    val (premise, street) = formatAddressText(addressText)
    val corporateName = name.toCorporateName()
    return findCorporateAddress(corporateName, addressText, postalCode)
      ?: let {
        corporateInsertRepository.insertCorporateIfNotExists(corporateName)
        val corporate = corporateRepository.findAllByCorporateName(corporateName).first()
        tapAddressInsertRepository.insertAddressIfNotExists("CORP", corporate.id, premise, street, postalCode)

        corporateRepository.findById(corporate.id).get()
          .addresses.first { (it.premise == premise && it.street == street && it.postalCode == postalCode) }
      }
  }

  private fun findAddressOrThrow(request: UpsertTemporaryAbsenceAddress, offender: Offender): Address {
    // If we have an address id then use that
    if (request.id != null) return addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    return when (request.name) {
      null -> findOffenderAddress(request.addressText, request.postalCode, offender)
      else -> findCorporateAddress(request.name, request.addressText, request.postalCode)
    }
      ?: throw BadDataException("Address not found")
  }

  private fun findOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress? {
    val (premise, street) = formatAddressText(addressText)
    return offenderAddressRepository.findFirstByOffender_RootOffenderIdAndPremiseAndStreetAndPostalCode(offender.rootOffenderId!!, premise, street, postalCode)
  }

  private fun findCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress? {
    val (premise, street) = formatAddressText(addressText)
    return corporateAddressRepository.findFirstByCorporate_CorporateNameAndPremiseAndStreetAndPostalCode(name.toCorporateName(), premise, street, postalCode)
  }

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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    prisonId = prison.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    absences = scheduledTemporaryAbsences.map {
      // Some old data has multiple scheduled IN movements for a single scheduled OUT - try to find the one with an actual movement IN
      val scheduledAbsenceReturn = it.scheduledTemporaryAbsenceReturns.firstOrNull { it.temporaryAbsenceReturn != null }
        ?: it.scheduledTemporaryAbsenceReturns.firstOrNull()
      val absenceReturn = scheduledAbsenceReturn?.temporaryAbsenceReturn
      Absence(
        scheduledTemporaryAbsence = it.toResponse(returnTime),
        scheduledTemporaryAbsenceReturn = scheduledAbsenceReturn?.toResponse(),
        temporaryAbsence = it.temporaryAbsence?.toResponse(),
        temporaryAbsenceReturn = absenceReturn?.toResponse(),
      )
    },
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsence.toResponse(applicationReturnTime: LocalDateTime) = ScheduledTemporaryAbsence(
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
    returnTime = returnTime ?: applicationReturnTime,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    contactPersonName = contactPersonName,
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
      toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
      toAddressPostcode = toAddress?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsenceReturn.toResponse(): TemporaryAbsenceReturn {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddress
      ?: scheduledTemporaryAbsence?.toAddress
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
      fromFullAddress = address?.toFullAddress(getAddressDescription(address)),
      fromAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsence.toTemporaryAbsenceResponse(): TemporaryAbsence {
    val address = toAddress ?: toAgency?.addresses?.firstOrNull()
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
      toAddressId = address?.addressId,
      toAddressOwnerClass = address?.addressOwnerClass,
      toAddressDescription = getAddressDescription(address),
      toFullAddress = address?.toFullAddress(getAddressDescription(address)) ?: toCity?.description,
      toAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsenceReturn.toTemporaryAbsenceReturnResponse(): TemporaryAbsenceReturn {
    val address = fromAddress ?: fromAgency?.addresses?.firstOrNull()
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
      fromFullAddress = address?.toFullAddress(getAddressDescription(fromAddress)) ?: fromCity?.description,
      fromAddressPostcode = address?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    prisonId = prison.id,
    toAgencyId = toAgency?.id,
    contactPersonName = contactPersonName,
    applicationType = applicationType.code,
    temporaryAbsenceType = temporaryAbsenceType?.code,
    temporaryAbsenceSubType = temporaryAbsenceSubType?.code,
    audit = toAudit(),
  )

  private fun OffenderScheduledTemporaryAbsence.toSingleResponse(applicationReturnTime: LocalDateTime) = ScheduledTemporaryAbsenceResponse(
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
    returnTime = returnTime ?: applicationReturnTime,
    toAddressId = toAddress?.addressId,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
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
    parentEventId = scheduledTemporaryAbsence.eventId,
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

  private fun OffenderTemporaryAbsence.toSingleResponse(): TemporaryAbsenceResponse {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress
      ?: toAgency?.addresses?.firstOrNull()
      ?: scheduledTemporaryAbsence?.toAddress
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
      fromPrison = fromAgency!!.id,
      toAgency = toAgency?.id,
      commentText = commentText,
      toAddressId = toAddress?.addressId,
      toAddressOwnerClass = toAddress?.addressOwnerClass,
      toAddressDescription = getAddressDescription(toAddress),
      toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)) ?: toCity?.description,
      toAddressPostcode = toAddress?.postalCode?.trim(),
      audit = toAudit(),
    )
  }

  private fun OffenderTemporaryAbsenceReturn.toSingleResponse(): TemporaryAbsenceReturnResponse {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: fromAgency?.addresses?.firstOrNull()
      ?: scheduledTemporaryAbsence?.temporaryAbsence?.toAddress
      ?: scheduledTemporaryAbsence?.toAddress
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
      toPrison = toAgency!!.id,
      commentText = commentText,
      fromAddressId = address?.addressId,
      fromAddressOwnerClass = address?.addressOwnerClass,
      fromAddressDescription = getAddressDescription(address),
      fromFullAddress = address?.toFullAddress(getAddressDescription(address)) ?: fromCity?.description,
      fromAddressPostcode = address?.postalCode?.trim(),
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

  private fun Address.toFullAddress(description: String?): String {
    val address = mutableListOf<String>()

    fun MutableList<String>.addIfNotEmpty(value: String?) {
      if (!value.isNullOrBlank()) {
        add(value.trim())
      }
    }

    // Append "Flat" if there is one
    if (!flat.isNullOrBlank()) {
      val flatText = if (flat!!.contains("flat", ignoreCase = true)) "" else "Flat "
      address.add("$flatText${flat!!.trim()}")
    }

    // remove corporate/agency description from any address elements that might contain it
    val cleanPremise = description?.let { premise?.replace(description, "") } ?: premise
    val cleanStreet = description?.let { street?.replace(description, "") } ?: street
    val cleanLocality = description?.let { locality?.replace(description, "") } ?: locality

    // Don't separate a numeric premise from the street, only if it's a name
    val hasPremise = !cleanPremise.isNullOrBlank()
    val premiseIsNumber = cleanPremise?.all { char -> char.isDigit() } ?: false
    val hasStreet = !cleanStreet.isNullOrBlank()
    when {
      hasPremise && premiseIsNumber && hasStreet -> address.add("$cleanPremise $cleanStreet")
      hasPremise && !premiseIsNumber && hasStreet -> address.add("$cleanPremise, $cleanStreet")
      hasPremise -> address.add(cleanPremise)
      hasStreet -> address.add(cleanStreet)
    }
    // Add others if they exist
    address.addIfNotEmpty(cleanLocality)
    address.addIfNotEmpty(city?.description)
    address.addIfNotEmpty(county?.description)
    address.addIfNotEmpty(country?.description)

    return address.joinToString(", ").trim()
  }

  private fun String.toCorporateName() = substring(0..(minOf(this.length, 40) - 1))
}

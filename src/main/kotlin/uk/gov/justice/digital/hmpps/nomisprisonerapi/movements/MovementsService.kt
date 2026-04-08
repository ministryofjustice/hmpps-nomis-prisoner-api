package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import jakarta.persistence.EntityManager
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.COMPLETED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapType
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
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
  private val offenderTapApplicationRepository: OffenderTapApplicationRepository,
  private val temporaryAbsenceRepository: OffenderTapMovementOutRepository,
  private val temporaryAbsenceReturnRepository: OffenderTapMovementInRepository,
  private val scheduledTemporaryAbsenceRepository: OffenderTapScheduleOutRepository,
  private val scheduledTemporaryAbsenceReturnRepository: OffenderTapScheduleInRepository,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val movementApplicationStatusRepository: ReferenceCodeRepository<MovementApplicationStatus>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val transportTypeRepository: ReferenceCodeRepository<TapTransportType>,
  private val addressRepository: AddressRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val movementApplicationTypeRepository: ReferenceCodeRepository<MovementApplicationType>,
  private val tapTypeRepository: ReferenceCodeRepository<TapType>,
  private val tapSubTypeRepository: ReferenceCodeRepository<TapSubType>,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val arrestAgencyRepository: ReferenceCodeRepository<ArrestAgency>,
  private val corporateAddressRepository: CorporateAddressRepository,
  private val agencyLocationAddressRepository: AgencyLocationAddressRepository,
  private val offenderAddressRepository: OffenderAddressRepository,
  private val corporateRepository: CorporateRepository,
  private val corporateInsertRepository: CorporateInsertRepository,
  private val tapAddressInsertRepository: TapAddressInsertRepository,
  private val externalMovementsRepository: OffenderExternalMovementRepository,
  private val entityManager: EntityManager,
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

    val movementApplications = offenderTapApplicationRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .also { it.fixMergedSchedules() }
      .also { it.unlinkCorruptTemporaryAbsenceReturns() }

    // To find the unscheduled movements we get all movements and remove those linked to a TAP application. This is necessary because of corrupt movements linked to the wrong application.
    val unscheduledTemporaryAbsences = allTemporaryAbsences - movementApplications.temporaryAbsences()
    val unscheduledTemporaryAbsenceReturns = allTemporaryAbsenceReturns - movementApplications.temporaryAbsenceReturns()

    data class Booking(val id: Long, val active: Boolean, val latest: Boolean)

    val bookings = (
      movementApplications.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        unscheduledTemporaryAbsences.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        unscheduledTemporaryAbsenceReturns.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) }
      ).toSet()

    return OffenderTemporaryAbsencesResponse(
      bookings = bookings.map { (bookingId, active, latest) ->
        toBookingTemporaryAbsences(
          bookingId = bookingId,
          active = active,
          latest = latest,
          movementApplications = movementApplications,
          unscheduledTemporaryAbsences = unscheduledTemporaryAbsences,
          unscheduledTemporaryAbsenceReturns = unscheduledTemporaryAbsenceReturns,
        )
      },
    )
  }

  fun getTemporaryAbsencesAndMovementsForBooking(bookingId: Long): BookingTemporaryAbsences {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")

    // We need to run these queries before findAllByOffenderBooking_BookingId otherwise if Hibernate finds an entity of
    // type OffenderTemporaryAbsence in the session where it's expecting an OffenderTemporaryAbsenceReturn it tries (and fails)
    // to use the wrong type instead of respecting the @NotFound(IGNORE). So we run these queries before the entity is loaded
    // into the session and the @NotFound is respected.
    val allTemporaryAbsences = temporaryAbsenceRepository.findAllByOffenderBooking_BookingId(bookingId)
    val allTemporaryAbsenceReturns = temporaryAbsenceReturnRepository.findAllByOffenderBooking_BookingId(bookingId)

    val movementApplications = offenderTapApplicationRepository.findAllByOffenderBooking_BookingId(bookingId)
      .also { it.fixMergedSchedules() }
      .also { it.unlinkCorruptTemporaryAbsenceReturns() }

    // To find the unscheduled movements we get all movements and remove those linked to a TAP application. This is necessary because of corrupt movements linked to the wrong application.
    val unscheduledTemporaryAbsences = allTemporaryAbsences - movementApplications.temporaryAbsences()
    val unscheduledTemporaryAbsenceReturns = allTemporaryAbsenceReturns - movementApplications.temporaryAbsenceReturns()

    return toBookingTemporaryAbsences(
      bookingId = bookingId,
      active = booking.active,
      latest = booking.bookingSequence == 1,
      movementApplications = movementApplications,
      unscheduledTemporaryAbsences = unscheduledTemporaryAbsences,
      unscheduledTemporaryAbsenceReturns = unscheduledTemporaryAbsenceReturns,
    )
  }

  private fun toBookingTemporaryAbsences(
    bookingId: Long,
    active: Boolean,
    latest: Boolean,
    movementApplications: List<OffenderTapApplication>,
    unscheduledTemporaryAbsences: List<OffenderTapMovementOut>,
    unscheduledTemporaryAbsenceReturns: List<OffenderTapMovementIn>,
  ) = BookingTemporaryAbsences(
    bookingId = bookingId,
    activeBooking = active,
    latestBooking = latest,
    temporaryAbsenceApplications = movementApplications.filter { it.offenderBooking.bookingId == bookingId }
      .map { it.toResponse() },
    unscheduledTemporaryAbsences = unscheduledTemporaryAbsences.filter { it.offenderBooking.bookingId == bookingId }
      .map { mov -> mov.toTemporaryAbsenceResponse() },
    unscheduledTemporaryAbsenceReturns = unscheduledTemporaryAbsenceReturns.filter { it.offenderBooking.bookingId == bookingId }
      .map { mov -> mov.toTemporaryAbsenceReturnResponse() },
  )

  private fun List<OffenderTapApplication>.fixMergedSchedules() {
    // find any scheduled returns on the wrong application and remove them (these are created during merges)
    val scheduledReturnsWithWrongParent = this.flatMap { application ->
      application.tapScheduleOuts.flatMap { it.tapScheduleIns }
        .filter { it.tapApplication != null && it.tapApplication != application }
    }

    // detach the scheduled returns with wrong application from their scheduled absence
    this.flatMap { it.tapScheduleOuts }.forEach {
      it.tapScheduleIns.removeAll(scheduledReturnsWithWrongParent)
    }

    // put the scheduled returns onto the correct application and schedule
    scheduledReturnsWithWrongParent.forEach { mergedReturn ->
      this.find { it.tapApplicationId == mergedReturn.tapApplication?.tapApplicationId }
        ?.tapScheduleOuts
        ?.firstOrNull { absence -> absence.tapScheduleIns.isEmpty() && absence.returnDate == mergedReturn.eventDate }
        ?.tapScheduleIns += mergedReturn
      mergedReturn.tapApplication = null
    }
  }

  /*
   * Cut links to any return movements that have ended up on the wrong application due to corrupt data
   */
  private fun List<OffenderTapApplication>.unlinkCorruptTemporaryAbsenceReturns() {
    forEach {
      it.tapScheduleOuts.forEach {
        it.tapScheduleIns.forEach { scheduledReturn ->
          scheduledReturn.tapMovementIn?.also { returnMovement ->
            if (returnMovement.unlinkWrongSchedules()) {
              scheduledReturn.tapMovementIn = null
            }
          }
        }
      }
    }
  }

  private fun List<OffenderTapApplication>.temporaryAbsences() = flatMap {
    it.tapScheduleOuts.mapNotNull {
      it.tapMovementOut
    }
  }.toSet()

  private fun List<OffenderTapApplication>.temporaryAbsenceReturns() = flatMap {
    it.tapScheduleOuts.flatMap {
      it.tapScheduleIns.mapNotNull {
        it.tapMovementIn
      }
    }
  }.toSet()

  fun getTemporaryAbsenceApplication(offenderNo: String, applicationId: Long): TemporaryAbsenceApplicationResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val application = offenderTapApplicationRepository.findByTapApplicationIdAndOffenderBooking_Offender_NomsId(applicationId, offenderNo)
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
      .filterNot { it.hasNullValues() }
      .map { findOrCreateAddress(it, offenderBooking.offender) }
      .firstOrNull()
    val toAddressAgency = toAddress
      .takeIf { it?.addressOwnerClass == "AGY" }
      ?.let { it as AgencyLocationAddress }
      ?.agencyLocation

    val application = request.movementApplicationId
      ?.let {
        offenderTapApplicationRepository.findByIdOrNullForUpdate(request.movementApplicationId)
          ?: throw NotFoundException("Temporary absence application with id=$request.movementApplicationId not found for offender with nomsId=$offenderNo")
      } ?: OffenderTapApplication(
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
      this.tapType = temporaryAbsenceType
      this.tapSubType = temporaryAbsenceSubType
      this.eventSubType = eventSubType
      this.toAgency = toAddressAgency
      this.toAddress = toAddress
      this.toAddressOwnerClass = toAddress?.addressOwnerClass
    }

    if (application.isApproved() && application.toAddress == null) {
      throw BadDataException("The application is approved but no address has been provided")
    }

    // NOMIS does not allow schedules on an unapproved single application - if we find one then remove it
    if (!application.isApproved() && application.isSingle()) {
      val schedule = application.tapScheduleOuts.firstOrNull()
      if (schedule?.tapMovementOut != null) throw BadDataException("Attempt to remove a schedule from an unapproved application failed - the schedule (${schedule.eventId}) is attached to a movement (${schedule.tapMovementOut!!.id.offenderBooking.bookingId}/${schedule.tapMovementOut!!.id.sequence}).")
      application.tapScheduleOuts.remove(schedule)
    }

    return offenderTapApplicationRepository.save(application)
      .let { UpsertTemporaryAbsenceApplicationResponse(offenderBooking.bookingId, it.tapApplicationId) }
  }

  @Transactional
  fun deleteTemporaryAbsenceApplication(offenderNo: String, applicationId: Long) {
    offenderTapApplicationRepository.findByIdOrNull(applicationId)
      ?.also { application ->
        if (application.tapScheduleOuts.isNotEmpty()) {
          throw ConflictException("Cannot delete temporary absence applicationId $applicationId because it has scheduled temporary absences")
        }

        offenderBookingRepository.findByIdOrNull(application.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("ApplicationId $applicationId exists on a different offender") }

        offenderTapApplicationRepository.delete(application)
      }
  }

  fun getScheduledTemporaryAbsence(offenderNo: String, eventId: Long): ScheduledTemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val scheduledAbsence = scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offenderNo)
      ?: throw NotFoundException("Scheduled temporary absence with eventId=$eventId not found for offender with nomsId=$offenderNo")

    return scheduledAbsence.toSingleResponse(scheduledAbsence.tapApplication.returnTime)
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

    if (request.eventId == null) {
      if (application.isUnapproved()) {
        throw ConflictException("Cannot add schedules to application ${application.tapApplicationId} because it's not approved (applicationStatus=${application.applicationStatus.code})")
      }
      if (application.isSingle() && application.hasSchedules()) {
        throw ConflictException("Cannot add schedules to application ${application.tapApplicationId} because it already has a single schedule")
      }
    }

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
          OffenderTapScheduleIn(
            offenderBooking = offenderBooking,
            tapApplication = application,
            tapScheduleOut = schedule,
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

    schedule.tapScheduleIns = scheduledReturn?.let { mutableListOf(scheduledReturn) } ?: mutableListOf()

    scheduledTemporaryAbsenceRepository.save(schedule)

    return UpsertScheduledTemporaryAbsenceResponse(
      bookingId = offenderBooking.bookingId,
      movementApplicationId = application.tapApplicationId,
      eventId = schedule.eventId,
      addressId = toAddress.addressId,
      addressOwnerClass = toAddress.addressOwnerClass,
    )
  }

  @Transactional
  fun deleteScheduledTemporaryAbsence(offenderNo: String, eventId: Long) {
    scheduledTemporaryAbsenceRepository.findByIdOrNull(eventId)
      ?.also { schedule ->
        if (schedule.tapMovementOut != null) {
          throw ConflictException("Cannot delete scheduled temporary absence eventId $eventId because it has a movement ${schedule.tapMovementOut!!.id.offenderBooking.bookingId} / ${schedule.tapMovementOut!!.id.sequence}}")
        }
        if (schedule.eventStatus.code == COMPLETED) {
          throw ConflictException("Cannot delete scheduled temporary absence eventId $eventId because it has status $COMPLETED")
        }
        if (schedule.tapScheduleIns.isNotEmpty()) {
          throw ConflictException("Cannot delete scheduled temporary absence eventId $eventId because it has an inbound schedule ${schedule.tapScheduleIns[0].eventId}")
        }

        offenderBookingRepository.findByIdOrNull(schedule.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("EventId $eventId exists on a different offender") }

        scheduledTemporaryAbsenceRepository.delete(schedule)
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

  fun getTemporaryAbsence(offenderNo: String, bookingId: Long, movementSeq: Int): TemporaryAbsenceResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val temporaryAbsence = temporaryAbsenceRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSeq)
      ?: throw NotFoundException("Temporary absence with bookingId=$bookingId and sequence=$movementSeq not found for offender with nomsId=$offenderNo")

    // Despite being non-nullable, Hibernate happily loads a null application if missing in NOMIS - if so then make the movement unscheduled
    if (temporaryAbsence.tapScheduleOut?.tapApplication == null) {
      temporaryAbsence.tapScheduleOut = null
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

    return OffenderTapMovementOut(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      tapScheduleOut = scheduledTemporaryAbsence,
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
    if (temporaryAbsenceReturn.tapScheduleOut?.tapApplication == null) {
      temporaryAbsenceReturn.tapScheduleOut = null
      temporaryAbsenceReturn.tapScheduleIn = null
    }

    temporaryAbsenceReturn.unlinkWrongSchedules()
    return temporaryAbsenceReturn.toSingleResponse()
  }

  // Make a temporary absence return unscheduled if the schedules have been corrupted
  private fun OffenderTapMovementIn.unlinkWrongSchedules(): Boolean = when (tapScheduleIn) {
    null -> false
    in tapScheduleOut?.tapScheduleIns ?: emptyList() -> false
    else -> {
      tapScheduleOut = null
      tapScheduleIn = null
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

    return OffenderTapMovementIn(
      id = OffenderExternalMovementId(offenderBooking, offenderBooking.maxMovementSequence() + 1),
      tapScheduleIn = scheduledTemporaryAbsenceReturn,
      tapScheduleOut = scheduledTemporaryAbsenceReturn?.tapScheduleOut,
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
      applications = Applications(count = offenderTapApplicationRepository.countByOffenderBooking_Offender_NomsId(offenderNo)),
      scheduledOutMovements = ScheduledOut(count = scheduledTemporaryAbsenceRepository.countByOffenderBooking_Offender_NomsId_AndTapApplicationIsNotNull(offenderNo)),
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
      scheduleOutIds = it.bookings.flatMap {
        it.temporaryAbsenceApplications.flatMap {
          it.absences.flatMap {
            listOfNotNull(
              it.scheduledTemporaryAbsence?.eventId,
            )
          }
        }
      },
      scheduleInIds = it.bookings.flatMap {
        it.temporaryAbsenceApplications.flatMap {
          it.absences.flatMap {
            listOfNotNull(
              it.scheduledTemporaryAbsenceReturn?.eventId,
            )
          }
        }
      },
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

  private fun movementApplicationOrThrow(movementApplicationId: Long) = offenderTapApplicationRepository.findByIdOrNull(movementApplicationId)
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

  private fun transportTypeOrThrow(transportType: String) = transportTypeRepository.findByIdOrNull(TapTransportType.pk(transportType))
    ?: throw BadDataException("Transport type $transportType is invalid")

  private fun addressOrThrow(addressId: Long) = addressRepository.findByIdOrNull(addressId)
    ?: throw BadDataException("Address id $addressId is invalid")

  private fun findOrCreateAddress(request: UpsertTemporaryAbsenceAddress, offender: Offender): Address {
    // If we have an address id then use that
    if (request.id != null) return addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    with(request.copyAndTrim()) {
      return when (isCorporateAddress()) {
        true -> findOrCreateCorporateAddress(name!!, addressText!!, postalCode)
        false -> findOrCreateOffenderAddress(addressText!!, postalCode, offender)
      }
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

  // Note that this assumes the address was created in DPS... a fair assumption because if the address was created in NOMIS we have the ID so never need to find it
  private fun findOrCreateOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress {
    val (premise, street) = formatAddressText(addressText)
    tapAddressInsertRepository.insertAddressIfNotExists("OFF", offender.rootOffenderId!!, premise, street, postalCode)

    val address = offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)
      .first { it.matchesDpsAddress(premise, street, postalCode) }
    if (address.usages.none { it.addressUsage == rotlAddressType }) {
      address.apply {
        usages += AddressUsage(AddressUsageId(this, "ROTL"), true, rotlAddressType)
      }
    }

    return address
  }

  // Note that this assumes the address was created in DPS... a fair assumption because if the address was created in NOMIS we have the ID so never need to find it
  private fun findOrCreateCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress {
    val (premise, street) = formatAddressText(addressText)
    val corporateName = name.toCorporateName()
    return findCorporateAddress(corporateName, addressText, postalCode)
      ?: let {
        corporateInsertRepository.insertCorporateIfNotExists(corporateName)
        val corporate = corporateRepository.findAllByCorporateName(corporateName).first()
        tapAddressInsertRepository.insertAddressIfNotExists("CORP", corporate.id, premise, street, postalCode)
          .also { entityManager.refresh(corporate) }

        corporateRepository.findById(corporate.id).get()
          .addresses.first { it.matchesDpsAddress(premise, street, postalCode) }
      }
  }

  private fun Address.matchesDpsAddress(premise: String?, street: String?, postalCode: String?): Boolean = (this.premise == premise && this.street == street && this.postalCode == postalCode && flat == null && locality == null && city == null && county == null && country == null)

  private fun findAddressOrThrow(request: UpsertTemporaryAbsenceAddress, offender: Offender): Address {
    // If we have an address id then use that (which means the address was created in NOMIS or has already been syncd from DPS)
    if (request.id != null) return addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    with(request.copyAndTrim()) {
      return when (isCorporateAddress()) {
        true -> findCorporateAddress(name!!, addressText!!, postalCode)
        false -> findOffenderAddress(addressText!!, postalCode, offender)
      }
        ?: throw BadDataException("Address not found")
    }
  }

  private fun findOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress? = offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)
    .firstOrNull {
      it.toFullAddress(null) == addressText.trim() && it.postalCode == postalCode?.trim()
    }

  private fun findCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress? {
    val corporateName = name.toCorporateName()
    return corporateAddressRepository.findAllByCorporate_CorporateName(corporateName)
      .firstOrNull {
        // Need to check address with and without corporate name - it might be included on a DPS address
        (it.toFullAddress(corporateName) == addressText.trim() || it.toFullAddress() == addressText.trim()) &&
          it.postalCode == postalCode?.trim()
      }
  }

  private fun agencyLocationOrThrow(agencyId: String) = agencyLocationRepository.findByIdOrNull(agencyId)
    ?: throw BadDataException("Agency id $agencyId is invalid")

  private fun movementApplicationTypeOrThrow(applicationType: String) = movementApplicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(applicationType))
    ?: throw BadDataException("Application type $applicationType is invalid")

  private fun temporaryAbsenceTypeOrThrow(temporaryAbsenceType: String) = tapTypeRepository.findByIdOrNull(TapType.pk(temporaryAbsenceType))
    ?: throw BadDataException("Temporary absence type $temporaryAbsenceType is invalid")

  private fun temporaryAbsenceSubTypeOrThrow(temporaryAbsenceSubType: String) = tapSubTypeRepository.findByIdOrNull(TapSubType.pk(temporaryAbsenceSubType))
    ?: throw BadDataException("Temporary absence sub type $temporaryAbsenceSubType is invalid")

  private fun eventStatusOrThrow(eventStatus: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(eventStatus))
    ?: throw BadDataException("Event status $eventStatus is invalid")

  private fun arrestAgencyOrThrow(arrestAgency: String) = arrestAgencyRepository.findByIdOrNull(ArrestAgency.pk(arrestAgency))
    ?: throw BadDataException("Arrest Agency $arrestAgency is invalid")

  private fun OffenderTapApplication.toResponse() = TemporaryAbsenceApplication(
    movementApplicationId = tapApplicationId,
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
    temporaryAbsenceType = tapType?.code,
    temporaryAbsenceSubType = tapSubType?.code,
    absences = tapScheduleOuts.map {
      // Some old data has multiple scheduled IN movements for a single scheduled OUT - try to find the one with an actual movement IN
      val scheduledAbsenceReturn = it.tapScheduleIns.firstOrNull { it.tapMovementIn != null }
        ?: it.tapScheduleIns.firstOrNull()
      val absenceReturn = scheduledAbsenceReturn?.tapMovementIn
      Absence(
        scheduledTemporaryAbsence = it.toResponse(returnTime),
        scheduledTemporaryAbsenceReturn = scheduledAbsenceReturn?.toResponse(),
        temporaryAbsence = it.tapMovementOut?.toResponse(),
        temporaryAbsenceReturn = absenceReturn?.toResponse(),
      )
    },
    audit = toAudit(),
  )

  private fun OffenderTapScheduleOut.toResponse(applicationReturnTime: LocalDateTime) = ScheduledTemporaryAbsence(
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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    contactPersonName = contactPersonName,
    audit = toAudit(),
  )

  private fun OffenderTapScheduleIn.toResponse() = ScheduledTemporaryAbsenceReturn(
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

  private fun OffenderTapMovementOut.toResponse(): TemporaryAbsence {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress ?: tapScheduleOut?.toAddress
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

  private fun OffenderTapMovementIn.toResponse(): TemporaryAbsenceReturn {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: tapScheduleOut?.tapMovementOut?.toAddress
      ?: tapScheduleOut?.toAddress
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

  private fun OffenderTapMovementOut.toTemporaryAbsenceResponse(): TemporaryAbsence {
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

  private fun OffenderTapMovementIn.toTemporaryAbsenceReturnResponse(): TemporaryAbsenceReturn {
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

  private fun OffenderTapApplication.toSingleResponse() = TemporaryAbsenceApplicationResponse(
    bookingId = offenderBooking.bookingId,
    activeBooking = offenderBooking.active,
    latestBooking = offenderBooking.bookingSequence == 1,
    movementApplicationId = tapApplicationId,
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
    temporaryAbsenceType = tapType?.code,
    temporaryAbsenceSubType = tapSubType?.code,
    audit = toAudit(),
  )

  private fun OffenderTapScheduleOut.toSingleResponse(applicationReturnTime: LocalDateTime) = ScheduledTemporaryAbsenceResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = tapApplication.tapApplicationId,
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
    toAddressDescription = getAddressDescription(toAddress),
    toFullAddress = toAddress?.toFullAddress(getAddressDescription(toAddress)),
    toAddressPostcode = toAddress?.postalCode?.trim(),
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    contactPersonName = contactPersonName,
    temporaryAbsenceType = tapApplication.tapType?.code,
    temporaryAbsenceSubType = tapApplication.tapSubType?.code,
    audit = toAudit(
      toAddress?.modifyDatetime?.takeIf { it.isAfter(modifyDatetime ?: createDatetime) },
      toAddress?.modifyDatetime?.takeIf { it.isAfter(modifyDatetime ?: createDatetime) }?.let { toAddress?.modifyUserId },
    ),
  )

  private fun OffenderTapScheduleIn.toSingleResponse() = ScheduledTemporaryAbsenceReturnResponse(
    bookingId = offenderBooking.bookingId,
    movementApplicationId = tapScheduleOut.tapApplication.tapApplicationId,
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

  private fun OffenderTapMovementOut.toSingleResponse(): TemporaryAbsenceResponse {
    // The address may only exist on the schedule so check there too
    val toAddress = toAddress
      ?: toAgency?.addresses?.firstOrNull()
      ?: tapScheduleOut?.toAddress
    return TemporaryAbsenceResponse(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      scheduledTemporaryAbsenceId = tapScheduleOut?.eventId,
      movementApplicationId = tapScheduleOut?.tapApplication?.tapApplicationId,
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

  private fun OffenderTapMovementIn.toSingleResponse(): TemporaryAbsenceReturnResponse {
    // The address may only exist on the outbound movement or the scheduled outbound movement so check there too
    val address = fromAddress
      ?: fromAgency?.addresses?.firstOrNull()
      ?: tapScheduleOut?.tapMovementOut?.toAddress
      ?: tapScheduleOut?.toAddress
    return TemporaryAbsenceReturnResponse(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      scheduledTemporaryAbsenceId = tapScheduleOut?.eventId,
      scheduledTemporaryAbsenceReturnId = tapScheduleIn?.eventId,
      movementApplicationId = tapScheduleOut?.tapApplication?.tapApplicationId,
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

  private fun Address.toFullAddress(description: String? = null): String {
    val address = mutableListOf<String>()

    fun MutableList<String>.addIfNotEmpty(value: String?) {
      if (!value.isNullOrBlank()) {
        add(value.trim())
      }
    }

    fun String?.cleanAddressComponent(): String? {
      // remove corporate/agency description from any address elements that might contain it
      fun String?.withoutDescription() = description?.let { this?.replace(description, "") } ?: this

      // remove trailing commas because they will be added back when the components are joined together
      fun String?.withoutTrailingCommas() = this?.trim()?.trimEnd(',')

      return this.withoutDescription().withoutTrailingCommas()
    }

    // Append "Flat" if there is one
    if (!flat.isNullOrBlank()) {
      val flatText = if (flat!!.contains("flat", ignoreCase = true)) "" else "Flat "
      address.add("$flatText${flat!!.trim()}")
    }

    val cleanPremise = premise.cleanAddressComponent()
    val cleanStreet = street.cleanAddressComponent()
    val cleanLocality = locality.cleanAddressComponent()

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
      .takeIf { it.isNotBlank() }
      ?: description
      // If the NOMIS address is empty that's fine - it just won't match the address we're looking for. Maybe another address will match or if not we'll throw.
      ?: ""
  }

  private fun String.toCorporateName() = substring(0..(minOf(this.length, 40) - 1))
}

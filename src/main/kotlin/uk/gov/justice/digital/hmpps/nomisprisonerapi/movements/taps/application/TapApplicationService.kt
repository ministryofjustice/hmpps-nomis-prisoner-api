package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapAddressService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.TapHelpers.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.toFullAddress
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class TapApplicationService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val applicationRepository: OffenderTapApplicationRepository,
  private val tapAddressService: TapAddressService,
  private val tapHelpers: TapHelpers,
) {
  fun getTapApplication(offenderNo: String, applicationId: Long): TapApplication {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val application = applicationRepository.findByTapApplicationIdAndOffenderBooking_Offender_NomsId(applicationId, offenderNo)
      ?: throw NotFoundException("Tap application with id=$applicationId not found for offender with nomsId=$offenderNo")

    return application.toResponse()
  }

  @Transactional
  fun upsertTapApplication(offenderNo: String, request: UpsertTapApplication): UpsertTapApplicationResponse {
    val offenderBooking = tapHelpers.offenderBookingOrThrow(offenderNo)
    val eventSubType = tapHelpers.movementReasonOrThrow(request.eventSubType)
    val applicationStatus = tapHelpers.applicationStatusOrThrow(request.applicationStatus)
    val escort = request.escortCode?.let { tapHelpers.escortOrThrow(request.escortCode) }
    val transportType = request.transportType?.let { tapHelpers.transportTypeOrThrow(request.transportType) }
    val prison = tapHelpers.agencyLocationOrThrow(request.prisonId)
    val applicationType = tapHelpers.movementApplicationTypeOrThrow(request.applicationType)
    val tapType = request.tapType?.let { tapHelpers.tapTypeOrThrow(request.tapType) }
    val tapSubType = request.tapSubType?.let { tapHelpers.tapSubTypeOrThrow(request.tapSubType) }
    // Make sure all requested addresses exist, but just take the first one as NOMIS only supports a single address at the application level
    val toAddress = request.toAddresses
      .filterNot { it.hasNullValues() }
      .map { tapAddressService.findOrCreateAddress(it, offenderBooking.offender) }
      .firstOrNull()
    val toAddressAgency = toAddress
      .takeIf { it?.addressOwnerClass == "AGY" }
      ?.let { it as AgencyLocationAddress }
      ?.agencyLocation

    val application = request.tapApplicationId
      ?.let {
        applicationRepository.findByIdOrNullForUpdate(request.tapApplicationId)
          ?: throw NotFoundException("Tap application with id=$request.movementApplicationId not found for offender with nomsId=$offenderNo")
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
      this.tapType = tapType
      this.tapSubType = tapSubType
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

    return applicationRepository.save(application)
      .let { UpsertTapApplicationResponse(offenderBooking.bookingId, it.tapApplicationId) }
  }

  @Transactional
  fun deleteTapApplication(offenderNo: String, applicationId: Long) {
    applicationRepository.findByIdOrNull(applicationId)
      ?.also { application ->
        if (application.tapScheduleOuts.isNotEmpty()) {
          throw ConflictException("Cannot delete tap applicationId $applicationId because it has tap schedules")
        }

        offenderBookingRepository.findByIdOrNull(application.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("ApplicationId $applicationId exists on a different offender") }

        applicationRepository.delete(application)
      }
  }

  private fun OffenderTapApplication.toResponse() = TapApplication(
    bookingId = offenderBooking.bookingId,
    activeBooking = offenderBooking.active,
    latestBooking = offenderBooking.bookingSequence == 1,
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
    audit = toAudit(),
  )
}

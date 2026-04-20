package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ArrestAgency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementTypeAndReasonId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MovementTypeAndReasonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
class TapHelpers(
  private val addressRepository: AddressRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val arrestAgencyRepository: ReferenceCodeRepository<ArrestAgency>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val movementApplicationStatusRepository: ReferenceCodeRepository<MovementApplicationStatus>,
  private val movementApplicationTypeRepository: ReferenceCodeRepository<MovementApplicationType>,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val movementTypeAndReasonRepository: MovementTypeAndReasonRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderTapApplicationRepository: OffenderTapApplicationRepository,
  private val tapScheduleOutRepository: OffenderTapScheduleOutRepository,
  private val tapScheduleInRepository: OffenderTapScheduleInRepository,
  private val tapSubTypeRepository: ReferenceCodeRepository<TapSubType>,
  private val tapTypeRepository: ReferenceCodeRepository<TapType>,
  private val transportTypeRepository: ReferenceCodeRepository<TapTransportType>,
) {

  companion object {
    val MAX_TAP_COMMENT_LENGTH = 225
  }

  fun offenderOrThrow(offenderNo: String) = offenderRepository.findRootByNomsId(offenderNo)
    ?: throw NotFoundException("Offender $offenderNo not found")

  fun offenderBookingOrThrow(offenderNo: String) = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("Offender $offenderNo not found with a booking")

  fun movementApplicationOrThrow(movementApplicationId: Long) = offenderTapApplicationRepository.findByIdOrNull(movementApplicationId)
    ?: throw BadDataException("Movement application $movementApplicationId does not exist")

  fun tapScheduleOutOrThrow(eventId: Long) = tapScheduleOutRepository.findByIdOrNull(eventId)
    ?: throw BadDataException("Tap schedule out $eventId does not exist")

  fun tapScheduleInOrThrow(eventId: Long) = tapScheduleInRepository.findByIdOrNull(eventId)
    ?: throw BadDataException("Tap schedule in $eventId does not exist")

  fun movementReasonOrThrow(movementReason: String) = movementReasonRepository.findByIdOrNull(MovementReason.pk(movementReason))
    ?: throw BadDataException("Event sub type $movementReason is invalid")

  fun movementTypeAndReasonOrThrow(movementType: String, movementReason: String) = movementTypeAndReasonRepository.findByIdOrNull(MovementTypeAndReasonId(movementType, movementReason))
    ?: throw BadDataException("Event type $movementType and sub type $movementReason is invalid")

  fun applicationStatusOrThrow(applicationStatus: String) = movementApplicationStatusRepository.findByIdOrNull(MovementApplicationStatus.pk(applicationStatus))
    ?: throw BadDataException("Application status $applicationStatus is invalid")

  fun escortOrThrow(escort: String) = escortRepository.findByIdOrNull(Escort.pk(escort))
    ?: throw BadDataException("Escort code $escort is invalid")

  fun transportTypeOrThrow(transportType: String) = transportTypeRepository.findByIdOrNull(TapTransportType.pk(transportType))
    ?: throw BadDataException("Transport type $transportType is invalid")

  fun addressOrThrow(addressId: Long) = addressRepository.findByIdOrNull(addressId)
    ?: throw BadDataException("Address id $addressId is invalid")

  fun agencyLocationOrThrow(agencyId: String) = agencyLocationRepository.findByIdOrNull(agencyId)
    ?: throw BadDataException("Agency id $agencyId is invalid")

  fun movementApplicationTypeOrThrow(applicationType: String) = movementApplicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(applicationType))
    ?: throw BadDataException("Application type $applicationType is invalid")

  fun tapTypeOrThrow(tapType: String) = tapTypeRepository.findByIdOrNull(TapType.pk(tapType))
    ?: throw BadDataException("Tap type $tapType is invalid")

  fun tapSubTypeOrThrow(tapSubType: String) = tapSubTypeRepository.findByIdOrNull(TapSubType.pk(tapSubType))
    ?: throw BadDataException("Tap sub type $tapSubType is invalid")

  fun eventStatusOrThrow(eventStatus: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(eventStatus))
    ?: throw BadDataException("Event status $eventStatus is invalid")

  fun arrestAgencyOrThrow(arrestAgency: String) = arrestAgencyRepository.findByIdOrNull(ArrestAgency.pk(arrestAgency))
    ?: throw BadDataException("Arrest Agency $arrestAgency is invalid")
}

internal fun List<OffenderTapApplication>.tapMovementOuts() = flatMap {
  it.tapScheduleOuts.mapNotNull {
    it.tapMovementOut
  }
}.toSet()

internal fun List<OffenderTapApplication>.tapMovementIns() = flatMap {
  it.tapScheduleOuts.flatMap {
    it.tapScheduleIns.mapNotNull {
      it.tapMovementIn
    }
  }
}.toSet()

internal fun OffenderTapMovementIn.unlinkWrongSchedules(): Boolean = when (tapScheduleIn) {
  null -> false
  in tapScheduleOut?.tapScheduleIns ?: emptyList() -> false
  else -> {
    tapScheduleOut = null
    tapScheduleIn = null
    true
  }
}

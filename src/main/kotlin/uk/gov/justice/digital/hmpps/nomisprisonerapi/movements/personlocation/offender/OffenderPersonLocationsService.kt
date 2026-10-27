package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.PersonLocationMovementMapper

@Service
@Transactional(readOnly = true)
class OffenderPersonLocationsService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val tapMovementOutRepository: OffenderTapMovementOutRepository,
  private val tapMovementInRepository: OffenderTapMovementInRepository,
  private val mapper: PersonLocationMovementMapper,
) {
  fun getOffenderPersonLocations(offenderNo: String): OffenderPersonLocationsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    // Loads TAP movements with their schedules and addresses in one go (the entity associations are eager) so the
    // booking's movements below are resolved from the persistence context rather than one query per movement
    tapMovementOutRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    tapMovementInRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    return OffenderPersonLocationsResponse(
      bookings = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).map { booking ->
        BookingPersonLocations(
          booking = mapper.toPersonLocationBooking(booking),
          movements = booking.externalMovements
            .filterNot { mapper.isIgnored(it) }
            .sortedBy { it.id.sequence }
            .map { mapper.toPersonLocationMovement(it) },
        )
      },
    )
  }
}

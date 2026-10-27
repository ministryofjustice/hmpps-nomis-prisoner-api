package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.movement

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.isIgnored
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.toPersonLocationMovement

@Service
@Transactional(readOnly = true)
class PersonLocationMovementService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val movementRepository: OffenderExternalMovementRepository,
) {
  fun getPersonLocationMovement(offenderNo: String, bookingId: Long, sequence: Int): PersonLocationMovement {
    val booking = findOffenderBookingOrThrow(offenderNo, bookingId)

    return movementRepository.findByIdOrNull(OffenderExternalMovementId(booking, sequence))
      ?.takeUnless { it.isIgnored() }
      ?.toPersonLocationMovement()
      ?: throw NotFoundException("Movement with bookingId=$bookingId and sequence=$sequence not found for offender with nomsId=$offenderNo")
  }

  private fun findOffenderBookingOrThrow(offenderNo: String, bookingId: Long): OffenderBooking {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    return offenderBookingRepository.findByIdOrNull(bookingId)
      ?.takeIf { it.offender.nomsId == offenderNo }
      ?: throw NotFoundException("Offender booking with bookingId=$bookingId not found for offender with nomsId=$offenderNo")
  }
}

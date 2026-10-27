package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.booking

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.toPersonLocationBooking

@Service
@Transactional(readOnly = true)
class PersonLocationBookingService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  fun getPersonLocationBooking(offenderNo: String, bookingId: Long): PersonLocationBooking {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    return offenderBookingRepository.findByIdOrNull(bookingId)
      ?.takeIf { it.offender.nomsId == offenderNo }
      ?.toPersonLocationBooking()
      ?: throw NotFoundException("Offender booking with bookingId=$bookingId not found for offender with nomsId=$offenderNo")
  }
}

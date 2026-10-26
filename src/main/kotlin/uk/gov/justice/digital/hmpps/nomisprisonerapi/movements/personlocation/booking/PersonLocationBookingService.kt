package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.booking

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PersonLocationBookingService {
  fun getPersonLocationBooking(offenderNo: String, bookingId: Long): PersonLocationBooking = TODO("Implemented in the next PR")
}

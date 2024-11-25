package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException

@Service
class IdentifyingMarksService {

  // TODO SDIT-2212 implement this method
  fun getIdentifyingMarks(bookingId: Long): BookingIdentifyingMarksResponse = throw NotFoundException("Booking not found: $bookingId")
}

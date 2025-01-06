package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository

@Service
@Transactional(readOnly = true)
class BookingService(private val offenderBookingRepository: OffenderBookingRepository) {
  fun findAllLatestBookingFromId(lastBookingId: Long, activeOnly: Boolean, pageSize: Int): BookingIdsWithLast {
    val bookings = if (activeOnly) offenderBookingRepository.findAllLatestActiveIdsFromId(bookingId = lastBookingId, pageSize = pageSize) else offenderBookingRepository.findAllLatestIdsFromId(bookingId = lastBookingId, pageSize = pageSize)

    return BookingIdsWithLast(
      prisonerIds = bookings.map { PrisonerIds(bookingId = it.getBookingId(), offenderNo = it.getPrisonerId()) },
      lastBookingId = bookings.lastOrNull()?.getBookingId() ?: 0,
    )
  }
}

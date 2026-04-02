package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional(readOnly = true)
class PrisonerSearchService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
) {
  fun findRootOffenderIdRanges(active: Boolean, pageSize: Int): List<RootOffenderIdRange> = mutableListOf(0L).apply {
    addAll(
      if (active) {
        bookingRepository.findEveryPageSizeActiveRootOffenderId(pageSize)
      } else {
        offenderRepository.findEveryPageSizeRootOffenderId(pageSize)
      },
    )
    add(Long.MAX_VALUE)
  }
    .zipWithNext().map { RootOffenderIdRange(it.first, it.second) }

  fun findPrisonNumbersInRange(active: Boolean, fromRootOffenderId: Long, toRootOffenderId: Long): List<String> = if (active) {
    bookingRepository.findActivePrisonNumbersBetweenIds(fromRootOffenderId, toRootOffenderId)
  } else {
    offenderRepository.findPrisonerIdsBetweenIds(fromRootOffenderId, toRootOffenderId)
      .map { it.getPrisonerId() }
  }

  fun getAllBookingsForPrisoner(prisonerNumber: String): List<Long> = offenderRepository
    .getAllBookingsForPrisoner(prisonerNumber).also { bookings ->
      if (bookings.isEmpty() && !offenderRepository.existsByNomsId(prisonerNumber)) {
        throw NotFoundException(prisonerNumber)
      }
    }
}

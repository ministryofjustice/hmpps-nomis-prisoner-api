package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional(readOnly = true)
class PrisonerSearchService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
) {
  fun findRootOffenderIdRanges(active: Boolean, pageSize: Int): List<RootOffenderIdRange> = (
    if (active) {
      bookingRepository.findEveryPageSizeActiveRootOffenderId(pageSize)
    } else {
      offenderRepository.findEveryPageSizeRootOffenderId(pageSize)
    } + Long.MAX_VALUE
    )
    .fold(Pair(0L, mutableListOf<RootOffenderIdRange>())) { acc, current ->
      Pair(
        current,
        acc.second.apply {
          add(RootOffenderIdRange(acc.first, current))
        },
      )
    }.second

  fun findPrisonNumbersInRange(active: Boolean, fromRootOffenderId: Long, toRootOffenderId: Long): List<String> = if (active) {
    bookingRepository.findActivePrisonNumbersBetweenIds(fromRootOffenderId, toRootOffenderId)
  } else {
    offenderRepository.findPrisonNumbersBetweenIds(fromRootOffenderId, toRootOffenderId)
  }
}

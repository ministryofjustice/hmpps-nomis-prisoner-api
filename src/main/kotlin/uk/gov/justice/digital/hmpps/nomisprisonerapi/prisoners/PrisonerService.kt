package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.ActiveBookingsSpecification
import java.time.LocalDate

@Service
class PrisonerService(
  private val bookingRepository: OffenderBookingRepository,
  private val mergeTransactionRepository: MergeTransactionRepository,
) {
  fun findAllActivePrisoners(pageRequest: Pageable): Page<PrisonerId> {
    return bookingRepository.findAll(ActiveBookingsSpecification(), pageRequest)
      .map { PrisonerId(it.bookingId, it.offender.nomsId) }
  }

  fun findPrisonerDetails(bookingIds: List<Long>): List<PrisonerDetails> {
    return bookingRepository.findAllById(bookingIds)
      .map { PrisonerDetails(it.offender.nomsId, it.bookingId, it.location?.id ?: "") }
  }

  fun findPrisonerMerges(offenderNo: String, fromDate: LocalDate?): List<MergeDetail> {
    return (
      fromDate
        ?.let { mergeTransactionRepository.findByNomsId1AndRequestDateGreaterThanEqual(offenderNo, it.atStartOfDay()) }
        ?: mergeTransactionRepository.findByNomsId1(offenderNo)
      )
      .map {
        MergeDetail(
          toOffenderNo = it.nomsId1,
          toBookingId = it.offenderBookId1,
          fromOffenderNo = it.nomsId2,
          fromBookingId = it.offenderBookId2,
          dateTime = it.requestDate,
        )
      }
  }
}

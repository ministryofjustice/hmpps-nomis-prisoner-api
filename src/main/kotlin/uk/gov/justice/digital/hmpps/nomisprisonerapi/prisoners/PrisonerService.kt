package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findRootByNomisId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.ActiveBookingsSpecification
import java.time.LocalDate

@Service
class PrisonerService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
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
    return mergeTransactionRepository.findByNomsIdAndAfterRequestDate(offenderNo, fromDate?.atStartOfDay())
      .map {
        val offender2Retained = offenderRepository.findRootByNomisId(it.nomsId2) != null
        MergeDetail(
          retainedOffenderNo = if (offender2Retained) it.nomsId2 else it.nomsId1,
          previousBookingId = it.offenderBookId1,
          deletedOffenderNo = if (offender2Retained) it.nomsId1 else it.nomsId2,
          activeBookingId = it.offenderBookId2,
          requestDateTime = it.requestDate,
        )
      }
  }
}

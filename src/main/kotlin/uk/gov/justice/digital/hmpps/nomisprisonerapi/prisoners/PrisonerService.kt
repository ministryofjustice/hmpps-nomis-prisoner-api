package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.ActiveBookingsSpecification

@Service
class PrisonerService(private val bookingRepository: OffenderBookingRepository) {
  fun findAllActivePrisoners(pageRequest: Pageable): Page<PrisonerId> {
    return bookingRepository.findAll(ActiveBookingsSpecification(), pageRequest)
      .map { PrisonerId(it.bookingId, it.offender.nomsId) }
  }

  fun findPrisonerDetails(bookingIds: List<Long>): List<PrisonerDetails> {
    return bookingRepository.findAllById(bookingIds)
      .map { PrisonerDetails(it.offender.nomsId, it.bookingId, it.location?.id ?: "") }
  }
}

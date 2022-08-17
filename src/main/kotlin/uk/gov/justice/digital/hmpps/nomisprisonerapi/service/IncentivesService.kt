package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.IncentiveIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.IncentiveFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncentiveSpecification

@Service
@Transactional
class IncentivesService(private val incentiveRepository: IncentiveRepository) {
  fun findIncentiveIdsByFilter(pageRequest: Pageable, incentiveFilter: IncentiveFilter): Page<IncentiveIdResponse> {
    return incentiveRepository.findAll(IncentiveSpecification(incentiveFilter), pageRequest)
      .map { IncentiveIdResponse(bookingId = it.id.offenderBooking.bookingId, sequence = it.id.sequence) }
  }
}

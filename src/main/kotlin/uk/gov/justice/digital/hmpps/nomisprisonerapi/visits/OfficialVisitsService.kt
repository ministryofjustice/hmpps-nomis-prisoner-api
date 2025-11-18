package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository

@Service
@Transactional
class OfficialVisitsService(
  private val visitRepository: VisitRepository,

) {
  fun getVisitIds(pageRequest: Pageable): Page<VisitIdResponse> = visitRepository.findAllOfficialVisitsIds(pageRequest).map {
    VisitIdResponse(
      visitId = it.id,
    )
  }
}

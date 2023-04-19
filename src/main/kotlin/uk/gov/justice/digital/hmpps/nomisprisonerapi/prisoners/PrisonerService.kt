package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PrisonerService {
  fun findAllPrisoners(pageRequest: Pageable, activeOnly: Boolean): Page<PrisonerId> {
    return Page.empty()
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository

@Service
@Transactional
class CorporateService(private val corporateRepository: CorporateRepository) {

  fun getCorporateById(corporateId: Long): CorporateOrganisation =
    corporateRepository.findByIdOrNull(corporateId)?.let {
      CorporateOrganisation(
        id = it.id,
        name = it.corporateName,
      )
    } ?: throw NotFoundException("Corporate not found $corporateId")
}

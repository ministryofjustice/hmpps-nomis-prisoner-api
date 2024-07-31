package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonIepLevelRepository

@Service
@Transactional
class PrisonService(
  private val agencyLocationRepository: AgencyLocationRepository,
  private val prisonIepLevelRepository: PrisonIepLevelRepository,
) {
  fun getPrisonIepLevels(prisonId: String): List<IncentiveLevel> =
    agencyLocationRepository.findByIdOrNull(prisonId)
      ?.let { prisonIepLevelRepository.findAllByAgencyLocationAndActive(it) }
      ?.map { IncentiveLevel(it.iepLevelCode, it.iepLevel.description) }
      ?: throw NotFoundException("Prison $prisonId does not exist")
}

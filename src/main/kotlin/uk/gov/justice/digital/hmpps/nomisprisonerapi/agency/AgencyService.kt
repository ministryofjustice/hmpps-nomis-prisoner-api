package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Agency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Prison
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonRepository

@Service
@Transactional
class AgencyService(
  val agencyLocationRepository: AgencyLocationRepository,
  val agencyRepository: AgencyRepository,
  val prisonRepository: PrisonRepository,
) {

  fun getPrison(prisonId: String): PrisonResponse = prisonRepository.findByIdOrNull(prisonId)
    ?.toPrisonResponse() ?: throw NotFoundException("Prison $prisonId does not exist")
  fun getAgency(agencyId: String): AgencyResponse = agencyRepository.findByIdOrNull(agencyId)
    ?.toAgencyResponse() ?: throw NotFoundException("Agency $agencyId does not exist")
  fun getAgencyLocation(agencyId: String): AgencyLocationResponse = agencyLocationRepository.findByIdOrNull(agencyId)
    ?.toAgencyLocationResponse() ?: throw NotFoundException("Agency $agencyId does not exist")
}

fun Prison.toPrisonResponse() = PrisonResponse(
  prisonId = this.id,
  description = this.description,
  district = this.district?.toCodeDescription(),
  active = this.active,
  deactivationDate = this.deactivationDate,
  updateAllowed = this.updateAllowed,
)

fun Agency.toAgencyResponse() = AgencyResponse(
  agencyId = this.id,
  description = this.description,
  district = this.district?.toCodeDescription(),
  active = this.active,
  deactivationDate = this.deactivationDate,
  type = this.type.toCodeDescription(),
  updateAllowed = this.updateAllowed,
)

fun AgencyLocation.toAgencyLocationResponse() = AgencyLocationResponse(
  agencyId = this.id,
  description = this.description,
  active = this.active,
  deactivationDate = this.deactivationDate,
  type = this.type.toCodeDescription(),
  updateAllowed = this.updateAllowed,
)

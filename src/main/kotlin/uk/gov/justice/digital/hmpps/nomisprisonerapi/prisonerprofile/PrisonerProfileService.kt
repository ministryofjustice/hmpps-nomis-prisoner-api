package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.PrisonerPhysicalAttributesResponse

@Service
class PrisonerProfileService {
  // TODO SDIT-1817 Implement this service
  fun getPhysicalAttributes(prisonerNumber: String) = PrisonerPhysicalAttributesResponse(prisonerNumber, listOf())
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.prisonerlocation.offender

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OffenderPrisonerLocationsService {
  fun getOffenderPrisonerLocations(offenderNo: String): Nothing = TODO()
}

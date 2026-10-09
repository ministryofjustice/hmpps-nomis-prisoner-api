package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OffenderPersonLocationsService {
  fun getOffenderPersonLocations(offenderNo: String): Nothing = TODO()
}

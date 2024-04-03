package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPIncidentLocation.CSIP_LOC)
class CSIPIncidentLocation(code: String, description: String) : ReferenceCode(CSIP_LOC, code, description) {
  companion object {
    const val CSIP_LOC = "CSIP_LOC"
    fun pk(code: String): Pk = Pk(CSIP_LOC, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPIncidentType.CSIP_TYP)
class CSIPIncidentType(code: String, description: String) : ReferenceCode(CSIP_TYP, code, description) {
  companion object {
    const val CSIP_TYP = "CSIP_TYP"
    fun pk(code: String): Pk = Pk(CSIP_TYP, code)
  }
}

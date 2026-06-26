package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CourtType.JURISDICTION)
class CourtType(code: String, description: String) : ReferenceCode(JURISDICTION, code, description) {

  companion object {
    const val JURISDICTION = "JURISDICTION"
    fun pk(code: String): Pk = Pk(JURISDICTION, code)
  }
}

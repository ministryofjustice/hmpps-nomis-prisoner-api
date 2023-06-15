package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationIncidentType.INC_TYPE)
class AdjudicationIncidentType(code: String, description: String) : ReferenceCode(INC_TYPE, code, description) {

  companion object {
    const val INC_TYPE = "INC_TYPE"
    const val GOVERNORS_REPORT = "GOV"
    fun pk(code: String): Pk = Pk(INC_TYPE, code)
  }
}

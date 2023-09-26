package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationFindingType.OIC_FINDING)
class AdjudicationFindingType(code: String, description: String) : ReferenceCode(OIC_FINDING, code, description) {

  companion object {
    const val OIC_FINDING = "OIC_FINDING"
    const val PROVED = "PROVED"
    fun pk(code: String): Pk = Pk(OIC_FINDING, code)
  }
}

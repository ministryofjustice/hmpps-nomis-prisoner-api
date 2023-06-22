package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationPleaFindingType.OIC_PLEA)
class AdjudicationPleaFindingType(code: String, description: String) : ReferenceCode(OIC_PLEA, code, description) {

  companion object {
    const val OIC_PLEA = "OIC_PLEA"
    fun pk(code: String): Pk = Pk(OIC_PLEA, code)
  }
}

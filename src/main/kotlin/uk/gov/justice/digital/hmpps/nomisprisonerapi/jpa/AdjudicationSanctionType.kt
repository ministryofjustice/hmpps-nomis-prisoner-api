package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationSanctionType.OIC_SANCT)
class AdjudicationSanctionType(code: String, description: String) : ReferenceCode(OIC_SANCT, code, description) {

  companion object {
    const val OIC_SANCT = "OIC_SANCT"
    fun pk(code: String): Pk = Pk(OIC_SANCT, code)
  }
}

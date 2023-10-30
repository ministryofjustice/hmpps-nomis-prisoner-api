package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationSanctionStatus.OIC_SANCT_ST)
class AdjudicationSanctionStatus(code: String, description: String) : ReferenceCode(OIC_SANCT_ST, code, description) {

  companion object {
    const val OIC_SANCT_ST = "OIC_SANCT_ST"
    const val QUASHED = "QUASHED"
    fun pk(code: String): Pk = Pk(OIC_SANCT_ST, code)
  }
}

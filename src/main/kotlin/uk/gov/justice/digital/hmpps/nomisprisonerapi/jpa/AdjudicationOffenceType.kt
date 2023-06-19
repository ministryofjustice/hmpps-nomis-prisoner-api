package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationOffenceType.OIC_OFN_TYPE)
class AdjudicationOffenceType(code: String, description: String) : ReferenceCode(OIC_OFN_TYPE, code, description) {
  companion object {
    const val OIC_OFN_TYPE = "OIC_OFN_TYPE"
    fun pk(code: String): Pk = Pk(OIC_OFN_TYPE, code)
  }
}

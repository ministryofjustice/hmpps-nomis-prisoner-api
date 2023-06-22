package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationHearingType.OIC_HEAR)
class AdjudicationHearingType(code: String, description: String) : ReferenceCode(OIC_HEAR, code, description) {

  companion object {
    const val OIC_HEAR = "OIC_HEAR"
    const val GOVERNORS_HEARING = "GOV"
    fun pk(code: String): Pk = Pk(OIC_HEAR, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationRepairType.REPAIR_TYPE)
class AdjudicationRepairType(code: String, description: String) : ReferenceCode(REPAIR_TYPE, code, description) {
  companion object {
    const val REPAIR_TYPE = "REPAIR_TYPE"
    fun pk(code: String): Pk = Pk(REPAIR_TYPE, code)
  }
}

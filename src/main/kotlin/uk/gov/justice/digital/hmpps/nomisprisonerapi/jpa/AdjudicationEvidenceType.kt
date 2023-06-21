package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AdjudicationEvidenceType.OIC_STMT_TYP)
class AdjudicationEvidenceType(code: String, description: String) : ReferenceCode(OIC_STMT_TYP, code, description) {

  companion object {
    const val OIC_STMT_TYP = "OIC_STMT_TYP"
    fun pk(code: String): Pk = Pk(OIC_STMT_TYP, code)
  }
}

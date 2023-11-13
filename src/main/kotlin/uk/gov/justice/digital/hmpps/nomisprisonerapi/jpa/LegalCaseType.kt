package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(LegalCaseType.LEG_CASE_TYP)
class LegalCaseType(code: String, description: String) : ReferenceCode(LEG_CASE_TYP, code, description) {
  companion object {
    const val LEG_CASE_TYP = "LEG_CASE_TYP"
    fun pk(code: String): Pk = Pk(LEG_CASE_TYP, code)
  }
}

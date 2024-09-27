package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CaseIdentifierType.CASE_ID_TYPE)
class CaseIdentifierType(code: String, description: String) : ReferenceCode(CASE_ID_TYPE, code, description) {
  companion object {
    const val CASE_ID_TYPE = "CASE_ID_TYPE"
    const val CASE_REFERENCE = "CASE/INFO#"
    fun pk(code: String): Pk = Pk(CASE_ID_TYPE, code)
  }
}

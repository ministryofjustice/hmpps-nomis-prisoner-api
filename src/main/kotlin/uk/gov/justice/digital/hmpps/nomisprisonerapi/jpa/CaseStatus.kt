package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CaseStatus.CASE_STS)
class CaseStatus(code: String, description: String) : ReferenceCode(CASE_STS, code, description) {
  companion object {
    const val CASE_STS = "CASE_STS"
    fun pk(code: String): Pk = Pk(CASE_STS, code)
  }
}

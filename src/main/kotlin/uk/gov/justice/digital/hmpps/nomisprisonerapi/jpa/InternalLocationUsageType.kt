package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(InternalLocationUsageType.ILOC_USAGE)
class InternalLocationUsageType(code: String, description: String) : ReferenceCode(ILOC_USAGE, code, description) {

  companion object {
    const val ILOC_USAGE = "ILOC_USG"
    fun pk(code: String): Pk = Pk(ILOC_USAGE, code)
  }
}

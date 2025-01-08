package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Ethnicity.ETHNICITY)
class Ethnicity(code: String, description: String) : ReferenceCode(ETHNICITY, code, description) {
  companion object {
    const val ETHNICITY = "ETHNICITY"
    fun pk(code: String): Pk = Pk(ETHNICITY, code)
  }
}

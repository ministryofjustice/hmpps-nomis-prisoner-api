package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(County.COUNTY)
class County(code: String, description: String) : ReferenceCode(COUNTY, code, description) {
  companion object {
    const val COUNTY = "COUNTY"
    fun pk(code: String): Pk = Pk(COUNTY, code)
  }
}

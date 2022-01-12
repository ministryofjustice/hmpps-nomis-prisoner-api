package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(Country.COUNTRY)
class Country(code: String, description: String) : ReferenceCode(COUNTRY, code, description) {
  companion object {
    const val COUNTRY = "COUNTRY"
    fun pk(code: String): Pk {
      return Pk(COUNTRY, code)
    }
  }
}

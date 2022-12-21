package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Country.COUNTRY)
class Country(code: String, description: String) : ReferenceCode(COUNTRY, code, description) {
  companion object {
    const val COUNTRY = "COUNTRY"
    fun pk(code: String): Pk = Pk(COUNTRY, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(City.CITY)
class City(code: String, description: String) : ReferenceCode(CITY, code, description) {
  companion object {
    const val CITY = "CITY"
    fun pk(code: String): Pk = Pk(CITY, code)
  }
}

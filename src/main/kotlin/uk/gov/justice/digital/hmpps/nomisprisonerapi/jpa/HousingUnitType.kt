package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(HousingUnitType.HOU_UN_TYPE)
class HousingUnitType(code: String, description: String) : ReferenceCode(HOU_UN_TYPE, code, description) {

  companion object {
    const val HOU_UN_TYPE = "HOU_UN_TYPE"
    fun pk(code: String): Pk = Pk(HOU_UN_TYPE, code)
  }
}

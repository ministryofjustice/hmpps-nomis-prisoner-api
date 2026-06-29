package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TypeOfArea.AREA_TYPE)
class TypeOfArea(code: String, description: String) : ReferenceCode(AREA_TYPE, code, description) {
  companion object {
    const val AREA_TYPE = "AREA_TYPE"
    fun pk(code: String): Pk = Pk(AREA_TYPE, code)
  }
}

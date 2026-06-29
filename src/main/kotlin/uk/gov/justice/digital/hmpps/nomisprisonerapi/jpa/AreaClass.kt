package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AreaClass.AREA_CLASS)
class AreaClass(code: String, description: String) : ReferenceCode(AREA_CLASS, code, description) {
  companion object {
    const val AREA_CLASS = "AREA_CLASS"
    fun pk(code: String): Pk = Pk(AREA_CLASS, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AreaType.AREA)
class AreaType(code: String, description: String) : ReferenceCode(AREA, code, description) {
  companion object {
    const val AREA = "AREA"
    fun pk(code: String): Pk = Pk(AREA, code)
  }
}

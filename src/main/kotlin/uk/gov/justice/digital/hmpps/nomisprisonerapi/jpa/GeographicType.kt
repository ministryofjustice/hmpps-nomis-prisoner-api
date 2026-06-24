package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(GeographicType.GEOGRAPHIC)
class GeographicType(code: String, description: String) : ReferenceCode(GEOGRAPHIC, code, description) {
  companion object {
    const val GEOGRAPHIC = "GEOGRAPHIC"
    fun pk(code: String): Pk = Pk(GEOGRAPHIC, code)
  }
}

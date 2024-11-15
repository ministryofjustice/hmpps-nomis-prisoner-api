package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Side.SIDE_CODE)
class Side(code: String, description: String) : ReferenceCode(SIDE_CODE, code, description) {

  companion object {
    const val SIDE_CODE = "SIDE"
    fun pk(code: String): Pk = Pk(SIDE_CODE, code)
  }
}

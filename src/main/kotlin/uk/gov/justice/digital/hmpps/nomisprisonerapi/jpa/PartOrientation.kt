package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(PartOrientation.PART_ORIENTATION_CODE)
class PartOrientation(code: String, description: String) : ReferenceCode(PART_ORIENTATION_CODE, code, description) {

  companion object {
    const val PART_ORIENTATION_CODE = "PART_ORIENT"
    fun pk(code: String): Pk = Pk(PART_ORIENTATION_CODE, code)
  }
}

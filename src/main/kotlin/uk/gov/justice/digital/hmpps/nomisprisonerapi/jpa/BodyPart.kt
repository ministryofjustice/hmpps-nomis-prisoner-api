package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(BodyPart.BODY_PART_CODE)
class BodyPart(code: String, description: String) : ReferenceCode(BODY_PART_CODE, code, description) {

  companion object {
    const val BODY_PART_CODE = "BODY_PART"
    fun pk(code: String): Pk = Pk(BODY_PART_CODE, code)
  }
}

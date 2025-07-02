package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TemporaryAbsenceType.TAP_ABS_TYPE)
class TemporaryAbsenceType(code: String, description: String) : ReferenceCode(TAP_ABS_TYPE, code, description) {

  companion object {
    const val TAP_ABS_TYPE = "TAP_ABS_TYPE"
    fun pk(code: String): Pk = Pk(TAP_ABS_TYPE, code)
  }
}

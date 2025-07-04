package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TemporaryAbsenceSubType.TAP_ABS_STYP)
class TemporaryAbsenceSubType(code: String, description: String) : ReferenceCode(TAP_ABS_STYP, code, description) {

  companion object {
    const val TAP_ABS_STYP = "TAP_ABS_STYP"
    fun pk(code: String): Pk = Pk(TAP_ABS_STYP, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(IncidentOffenderPartyRole.IR_OFF_PART)
class IncidentOffenderPartyRole(code: String, description: String) : ReferenceCode(IR_OFF_PART, code, description) {

  companion object {
    const val IR_OFF_PART = "IR_OFF_PART"
    fun pk(code: String): Pk = Pk(IR_OFF_PART, code)
  }
}

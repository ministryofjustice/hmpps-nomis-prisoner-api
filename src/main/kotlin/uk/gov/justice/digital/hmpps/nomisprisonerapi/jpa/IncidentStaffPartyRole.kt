package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(IncidentStaffPartyRole.IR_STF_PART)
class IncidentStaffPartyRole(code: String, description: String) : ReferenceCode(IR_STF_PART, code, description) {

  companion object {
    const val IR_STF_PART = "IR_STF_PART"
    fun pk(code: String): Pk = Pk(IR_STF_PART, code)
  }
}

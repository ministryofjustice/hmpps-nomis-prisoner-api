package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(IncidentType.IR_TYPE)
class IncidentType(code: String, description: String) : ReferenceCode(IR_TYPE, code, description) {

  companion object {
    const val IR_TYPE = "IR_TYPE"
    fun pk(code: String): Pk = Pk(IR_TYPE, code)
  }
}

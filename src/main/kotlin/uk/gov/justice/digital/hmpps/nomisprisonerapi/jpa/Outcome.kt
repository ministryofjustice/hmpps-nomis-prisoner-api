package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Outcome.IR_OUTCOME)
class Outcome(code: String, description: String) : ReferenceCode(IR_OUTCOME, code, description) {

  companion object {
    const val IR_OUTCOME = "IR_OUTCOME"
    fun pk(code: String): Pk = Pk(IR_OUTCOME, code)
  }
}

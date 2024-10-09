package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(EventOutcome.EVENT_OUTCOME)
class EventOutcome(code: String, description: String) : ReferenceCode(EVENT_OUTCOME, code, description) {
  companion object {
    const val EVENT_OUTCOME = "OUTCOMES"
    val ABS = pk("ABS")
    val ATT = pk("ATT")
    fun pk(code: String): Pk = Pk(EVENT_OUTCOME, code)
  }
}

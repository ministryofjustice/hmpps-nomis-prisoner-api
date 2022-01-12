package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(EventOutcome.EVENT_OUTCOME)
class EventOutcome(code: String, description: String) : ReferenceCode(EVENT_OUTCOME, code, description) {
  companion object {
    const val EVENT_OUTCOME = "OUTCOMES"
    fun pk(code: String): Pk {
      return Pk(EVENT_OUTCOME, code)
    }
  }
}

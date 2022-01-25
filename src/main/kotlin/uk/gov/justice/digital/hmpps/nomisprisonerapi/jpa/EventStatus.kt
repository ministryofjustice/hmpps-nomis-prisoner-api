package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(EventStatus.EVENT_STS)
class EventStatus(code: String, description: String) : ReferenceCode(EVENT_STS, code, description) {
  val isScheduled: Boolean
    get() = SCHEDULED == this.code

  companion object {
    const val EVENT_STS = "EVENT_STS"
    const val SCHEDULED = "SCH"
    val CANCELLED = Pk(EVENT_STS, "CANC")
    val SCHEDULED_APPROVED = Pk(EVENT_STS, SCHEDULED)
    val COMPLETED = Pk(EVENT_STS, "COMP")
  }
}

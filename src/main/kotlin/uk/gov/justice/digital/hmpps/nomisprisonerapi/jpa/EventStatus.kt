package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(EventStatus.EVENT_STS)
class EventStatus(code: String, description: String) : ReferenceCode(EVENT_STS, code, description) {
  val isScheduled: Boolean
    get() = SCHEDULED == this.code

  companion object {
    const val EVENT_STS = "EVENT_STS"
    const val SCHEDULED = "SCH"
    const val COMPLETED = "COMP"
    val CANCELLED = Pk(EVENT_STS, "CANC")
    val SCHEDULED_APPROVED = Pk(EVENT_STS, SCHEDULED)
    fun pk(code: String): Pk = Pk(EVENT_STS, code)
  }
}

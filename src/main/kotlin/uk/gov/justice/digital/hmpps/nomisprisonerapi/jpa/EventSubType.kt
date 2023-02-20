package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(EventSubType.INT_SCH_RSN)
class EventSubType(code: String, description: String) : ReferenceCode(INT_SCH_RSN, code, description) {
  companion object {
    // in general there are 3 domains for this reference data type:
    const val MOVE_RSN = "MOVE_RSN"
    const val INT_SCH_RSN = "INT_SCH_RSN"
    const val EVENT_SUBTYP = "EVENT_SUBTYP"
    // ... but entries of type APP and class INT_MOV are all from domain INT_SCH_RSN
    fun pk(code: String): Pk = Pk(INT_SCH_RSN, code)
  }
}

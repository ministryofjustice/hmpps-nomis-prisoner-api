package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CourtEventType.MOVE_RSN)
class CourtEventType(code: String, description: String) : ReferenceCode(MOVE_RSN, code, description) {
  companion object {
    const val MOVE_RSN = "MOVE_RSN"

    // ... but entries of type APP and class INT_MOV are all from domain INT_SCH_RSN
    fun pk(code: String): Pk = Pk(MOVE_RSN, code)
  }
}

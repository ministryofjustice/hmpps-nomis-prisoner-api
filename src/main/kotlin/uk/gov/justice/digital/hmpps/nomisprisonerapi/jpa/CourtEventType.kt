package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CourtEventType.MOVE_RSN)
class CourtEventType(code: String, description: String) : ReferenceCode(MOVE_RSN, code, description) {
  companion object {
    const val MOVE_RSN = "MOVE_RSN"

    fun pk(code: String): Pk = Pk(MOVE_RSN, code)
  }
}

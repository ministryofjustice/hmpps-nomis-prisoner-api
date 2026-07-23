package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MovementReason.MOVE_RSN)
class MovementReason(code: String, description: String) : ReferenceCode(MOVE_RSN, code, description) {

  companion object {
    const val MOVE_RSN = "MOVE_RSN"
    const val RECALL_BREACH_HEARING = "BREACH"
    fun pk(code: String): Pk = Pk(MOVE_RSN, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MovementType.MOVE_TYPE)
class MovementType(code: String, description: String) : ReferenceCode(MOVE_TYPE, code, description) {

  companion object {
    const val MOVE_TYPE = "MOVE_TYPE"
    fun pk(code: String): Pk = Pk(MOVE_TYPE, code)
  }
}

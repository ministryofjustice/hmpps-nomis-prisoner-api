package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(DirectionType.MOVE_DIRECT)
class DirectionType(code: String, description: String) : ReferenceCode(MOVE_DIRECT, code, description) {

  companion object {
    const val OUT = "OUT"
    const val MOVE_DIRECT = "MOVE_DIRECT"
    fun pk(code: String): Pk = Pk(MOVE_DIRECT, code)
  }
}

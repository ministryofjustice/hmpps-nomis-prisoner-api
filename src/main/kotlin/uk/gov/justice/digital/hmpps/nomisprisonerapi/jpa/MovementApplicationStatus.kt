package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MovementApplicationStatus.MOV_APP_STAT)
class MovementApplicationStatus(code: String, description: String) : ReferenceCode(MOV_APP_STAT, code, description) {

  companion object {
    const val MOV_APP_STAT = "MOV_APP_STAT"
    fun pk(code: String): Pk = Pk(MOV_APP_STAT, code)
  }
}

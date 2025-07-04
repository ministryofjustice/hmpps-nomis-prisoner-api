package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MovementApplicationType.MOV_APP_TYPE)
class MovementApplicationType(code: String, description: String) : ReferenceCode(MOV_APP_TYPE, code, description) {

  companion object {
    const val MOV_APP_TYPE = "MOV_APP_TYPE"
    fun pk(code: String): Pk = Pk(MOV_APP_TYPE, code)
  }
}

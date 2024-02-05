package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(InternalLocationType.ILOC_TYPE)
class InternalLocationType(code: String, description: String) : ReferenceCode(ILOC_TYPE, code, description) {

  companion object {
    const val ILOC_TYPE = "ILOC_TYPE"
    val VISIT = Pk(ILOC_TYPE, "VISIT")
    fun pk(code: String): Pk = Pk(ILOC_TYPE, code)
  }
}

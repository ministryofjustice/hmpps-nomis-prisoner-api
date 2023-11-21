package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(PleaStatusType.PLEA_STATUS)
class PleaStatusType(code: String, description: String) : ReferenceCode(PLEA_STATUS, code, description) {

  companion object {
    const val PLEA_STATUS = "PLEA_STATUS"
    fun pk(code: String): Pk = Pk(PLEA_STATUS, code)
  }
}

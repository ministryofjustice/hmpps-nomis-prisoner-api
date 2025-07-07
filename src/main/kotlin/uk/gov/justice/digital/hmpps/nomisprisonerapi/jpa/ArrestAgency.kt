package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(ArrestAgency.ARREST_AGY)
class ArrestAgency(code: String, description: String) : ReferenceCode(ARREST_AGY, code, description) {

  companion object {
    const val ARREST_AGY = "ARREST_AGY"
    fun pk(code: String): Pk = Pk(ARREST_AGY, code)
  }
}

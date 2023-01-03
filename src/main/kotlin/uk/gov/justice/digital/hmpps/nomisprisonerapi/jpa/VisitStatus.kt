package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(VisitStatus.VISIT_STATUS)
class VisitStatus(code: String, description: String) : ReferenceCode(VISIT_STATUS, code, description) {
  companion object {
    const val VISIT_STATUS = "VIS_STS"
    val NORM = pk("NORM")
    val CANCELLED = pk("CANC")
    fun pk(code: String): Pk = Pk(VISIT_STATUS, code)
  }
}

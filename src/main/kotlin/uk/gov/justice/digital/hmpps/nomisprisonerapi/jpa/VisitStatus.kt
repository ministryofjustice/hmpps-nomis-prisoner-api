package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitStatus.VISIT_STATUS)
class VisitStatus(code: String, description: String) : ReferenceCode(VISIT_STATUS, code, description) {
  companion object {
    const val VISIT_STATUS = "VIS_STS"
    val NORM = Pk(VISIT_STATUS, "NORM")
    fun pk(code: String): Pk {
      return Pk(VISIT_STATUS, code)
    }
  }
}

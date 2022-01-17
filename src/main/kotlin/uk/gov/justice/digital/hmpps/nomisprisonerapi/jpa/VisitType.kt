package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitType.VISIT_TYPE)
class VisitType(code: String, description: String) : ReferenceCode(VISIT_TYPE, code, description) {

  fun isSocial() = code == "SCON"
  companion object {
    const val VISIT_TYPE = "VISIT_TYPE"
    fun pk(code: String): Pk {
      return Pk(VISIT_TYPE, code)
    }
  }
}

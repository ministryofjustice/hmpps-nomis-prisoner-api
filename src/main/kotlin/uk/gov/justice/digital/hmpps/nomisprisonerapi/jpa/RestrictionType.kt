package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(RestrictionType.VST_RST_TYPE)
class RestrictionType(code: String, description: String) : ReferenceCode(VST_RST_TYPE, code, description) {

  companion object {
    const val VST_RST_TYPE = "VST_RST_TYPE"
    fun pk(code: String): Pk = Pk(VST_RST_TYPE, code)
  }
}

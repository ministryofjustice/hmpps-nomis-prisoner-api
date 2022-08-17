package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel.Companion.IEP_LEVEL
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(IEP_LEVEL)
class IEPLevel(code: String, description: String) : ReferenceCode(IEP_LEVEL, code, description) {
  companion object {
    const val IEP_LEVEL = "IEP_LEVEL"
    fun pk(code: String): Pk = Pk(IEP_LEVEL, code)
  }
}

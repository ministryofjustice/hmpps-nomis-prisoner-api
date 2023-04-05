package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel.Companion.IEP_LEVEL

@Entity
@DiscriminatorValue(IEP_LEVEL)
class IEPLevel(
  code: String,
  description: String,
  active: Boolean = true,
) :
  ReferenceCode(IEP_LEVEL, code, description, active) {
  companion object {
    const val IEP_LEVEL = "IEP_LEVEL"
    fun pk(code: String): Pk = Pk(IEP_LEVEL, code)
  }
}

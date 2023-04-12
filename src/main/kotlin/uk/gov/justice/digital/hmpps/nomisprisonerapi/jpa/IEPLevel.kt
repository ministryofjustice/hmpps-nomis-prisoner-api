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
  sequence: Int = 0,
) :
  ReferenceCode(IEP_LEVEL, code, description, active, sequence, sequence.toString()) {
  companion object {
    const val IEP_LEVEL = "IEP_LEVEL"
    fun pk(code: String): Pk = Pk(IEP_LEVEL, code)
  }
}

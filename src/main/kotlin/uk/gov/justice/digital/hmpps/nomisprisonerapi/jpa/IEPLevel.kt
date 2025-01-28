package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel.Companion.IEP_LEVEL
import java.time.LocalDate

@Entity
@DiscriminatorValue(IEP_LEVEL)
class IEPLevel(
  code: String,
  description: String,
  active: Boolean = true,
  sequence: Int = 0,
  expiredDate: LocalDate? = null,
) : ReferenceCode(
  domain = IEP_LEVEL,
  code = code,
  description = description,
  active = active,
  sequence = sequence,
  parentCode = sequence.toString(),
  parentDomain = null,
  expiredDate = expiredDate,
) {
  companion object {
    const val IEP_LEVEL = "IEP_LEVEL"
    fun pk(code: String): Pk = Pk(IEP_LEVEL, code)
  }
}

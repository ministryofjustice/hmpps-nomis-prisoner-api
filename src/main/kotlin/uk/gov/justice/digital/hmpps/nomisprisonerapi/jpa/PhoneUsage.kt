package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(PhoneUsage.PHONE_USAGE)
class PhoneUsage(code: String, description: String) : ReferenceCode(PHONE_USAGE, code, description) {
  companion object {
    const val PHONE_USAGE = "PHONE_USAGE"
    fun pk(code: String): Pk = Pk(PHONE_USAGE, code)
  }
}

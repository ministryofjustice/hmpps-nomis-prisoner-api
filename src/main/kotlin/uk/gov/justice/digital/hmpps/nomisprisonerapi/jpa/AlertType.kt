package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AlertType.DOMAIN)
class AlertType(
  code: String,
  description: String,
  sequence: Int = 0,
) : ReferenceCode(
  domain = DOMAIN,
  code = code,
  description = description,
  active = true,
  sequence = sequence,
  expiredDate = null,
  parentCode = null,
  parentDomain = null,
) {

  companion object {
    const val DOMAIN = "ALERT"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}

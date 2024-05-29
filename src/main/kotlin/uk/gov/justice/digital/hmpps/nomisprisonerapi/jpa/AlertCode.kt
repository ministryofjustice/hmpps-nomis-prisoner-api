package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AlertCode.DOMAIN)
class AlertCode(
  code: String,
  description: String,
  sequence: Int = 0,
  parentCode: String,
  parentDomain: String = AlertType.DOMAIN,
) : ReferenceCode(
  domain = DOMAIN,
  code = code,
  description = description,
  active = true,
  sequence = sequence,
  parentCode = parentCode,
  parentDomain = parentDomain,
  expiredDate = null,
) {

  companion object {
    const val DOMAIN = "ALERT_CODE"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}

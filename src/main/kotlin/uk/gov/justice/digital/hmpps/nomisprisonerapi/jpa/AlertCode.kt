package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AlertCode.DOMAIN)
class AlertCode(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {

  companion object {
    const val DOMAIN = "ALERT_CODE"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}

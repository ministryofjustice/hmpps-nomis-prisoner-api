package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AlertType.DOMAIN)
class AlertType(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {

  companion object {
    const val DOMAIN = "ALERT"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}

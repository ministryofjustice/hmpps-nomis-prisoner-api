package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TapTransportType.TA_TRANSPORT)
class TapTransportType(code: String, description: String) : ReferenceCode(TA_TRANSPORT, code, description) {
  companion object {
    const val TA_TRANSPORT = "TA_TRANSPORT"
    fun pk(code: String): Pk = Pk(TA_TRANSPORT, code)
  }
}

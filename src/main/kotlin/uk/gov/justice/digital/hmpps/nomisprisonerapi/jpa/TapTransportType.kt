package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TapTransportType.TAP_TRANSPORT_TYPE)
class TapTransportType(code: String, description: String) : ReferenceCode(TAP_TRANSPORT_TYPE, code, description) {
  companion object {
    const val TAP_TRANSPORT_TYPE = "TA_TRANSPORT"
    fun pk(code: String): Pk = Pk(TAP_TRANSPORT_TYPE, code)
  }
}

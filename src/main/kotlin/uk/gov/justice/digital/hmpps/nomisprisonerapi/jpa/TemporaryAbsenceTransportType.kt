package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TemporaryAbsenceTransportType.TA_TRANSPORT)
class TemporaryAbsenceTransportType(code: String, description: String) : ReferenceCode(TA_TRANSPORT, code, description) {
  companion object {
    const val TA_TRANSPORT = "TA_TRANSPORT"
    fun pk(code: String): Pk = Pk(TA_TRANSPORT, code)
  }
}

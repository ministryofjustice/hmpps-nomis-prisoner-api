package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TransferCancellationReason.TRANSFER_CANCELLATION_REASON)
class TransferCancellationReason(code: String, description: String) : ReferenceCode(TRANSFER_CANCELLATION_REASON, code, description) {
  companion object {
    const val TRANSFER_CANCELLATION_REASON = "TRN_CNCL_RSN"
    fun pk(code: String): Pk = Pk(TRANSFER_CANCELLATION_REASON, code)
  }
}

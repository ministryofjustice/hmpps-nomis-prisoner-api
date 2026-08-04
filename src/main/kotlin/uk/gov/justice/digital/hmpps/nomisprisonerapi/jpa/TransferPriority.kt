package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TransferPriority.TRN_PRIORITY)
class TransferPriority(code: String, description: String) : ReferenceCode(TRN_PRIORITY, code, description) {
  companion object {
    const val TRN_PRIORITY = "TRN_PRIORITY"
    fun pk(code: String): Pk = Pk(TRN_PRIORITY, code)
  }
}

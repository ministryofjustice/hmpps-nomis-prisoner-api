package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TransferScheduleStatus.TRN_SCH_STS)
class TransferScheduleStatus(code: String, description: String) : ReferenceCode(TRN_SCH_STS, code, description) {
  companion object {
    const val TRN_SCH_STS = "TRN_SCH_STS"
    fun pk(code: String): Pk = Pk(TRN_SCH_STS, code)
  }
}

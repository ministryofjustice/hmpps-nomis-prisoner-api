package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(InternalScheduleReason.INT_SCH_RSN)
class InternalScheduleReason(code: String, description: String) : ReferenceCode(INT_SCH_RSN, code, description) {
  companion object {
    const val INT_SCH_RSN = "INT_SCH_RSN"
    fun pk(code: String): Pk = Pk(INT_SCH_RSN, code)
  }
}

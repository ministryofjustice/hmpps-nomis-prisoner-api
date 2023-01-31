package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason.Companion.END_REASON

@Entity
@DiscriminatorValue(END_REASON)
class ProgramServiceEndReason(code: String, description: String) : ReferenceCode(END_REASON, code, description) {

  companion object {
    const val END_REASON = "PS_END_RSN"
    fun pk(code: String): Pk = Pk(END_REASON, code)
  }
}

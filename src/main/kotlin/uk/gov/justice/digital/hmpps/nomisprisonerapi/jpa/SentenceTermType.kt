package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SentenceTermType.SENT_TERM)
class SentenceTermType(code: String, description: String) : ReferenceCode(SENT_TERM, code, description) {

  companion object {
    const val SENT_TERM = "SENT_TERM"
    fun pk(code: String): Pk = Pk(SENT_TERM, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SentencePurposeType.PSR_PUR_SEN)
class SentencePurposeType(code: String, description: String) : ReferenceCode(PSR_PUR_SEN, code, description) {
  companion object {
    const val PSR_PUR_SEN = "PSR_PUR_SEN"

    fun pk(code: String): Pk = Pk(PSR_PUR_SEN, code)
  }
}

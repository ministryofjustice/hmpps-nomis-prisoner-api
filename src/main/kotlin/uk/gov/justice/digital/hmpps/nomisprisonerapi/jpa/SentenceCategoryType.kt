package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SentenceCategoryType.CATEGORY)
class SentenceCategoryType(code: String, description: String) : ReferenceCode(CATEGORY, code, description) {

  companion object {
    const val CATEGORY = "CATEGORY"
    fun pk(code: String): Pk = Pk(CATEGORY, code)
  }
}

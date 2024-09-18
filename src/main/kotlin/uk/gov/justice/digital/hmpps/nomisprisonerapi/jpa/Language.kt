package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Language.LANG)
class Language(code: String, description: String) : ReferenceCode(LANG, code, description) {
  companion object {
    const val LANG = "LANG"
    fun pk(code: String): Pk = Pk(LANG, code)
  }
}

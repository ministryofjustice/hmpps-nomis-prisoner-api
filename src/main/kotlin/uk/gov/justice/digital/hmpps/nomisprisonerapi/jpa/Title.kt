package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Title.TITLE)
class Title(code: String, description: String) : ReferenceCode(TITLE, code, description) {
  companion object {
    const val TITLE = "TITLE"
    fun pk(code: String): Pk = Pk(TITLE, code)
  }
}

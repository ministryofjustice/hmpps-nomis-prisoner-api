package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SearchLevel.SEARCH_LEVEL)
class SearchLevel(code: String, description: String) : ReferenceCode(SEARCH_LEVEL, code, description) {
  companion object {
    const val SEARCH_LEVEL = "SEARCH_LEVEL"
    fun pk(code: String): Pk = Pk(SEARCH_LEVEL, code)
  }
}

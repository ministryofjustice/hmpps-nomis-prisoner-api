package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(SearchLevel.SEARCH_LEVEL)
class SearchLevel(code: String, description: String) : ReferenceCode(SEARCH_LEVEL, code, description) {
  companion object {
    const val SEARCH_LEVEL = "SEARCH_LEVEL"
    fun pk(code: String): Pk {
      return Pk(SEARCH_LEVEL, code)
    }
  }
}

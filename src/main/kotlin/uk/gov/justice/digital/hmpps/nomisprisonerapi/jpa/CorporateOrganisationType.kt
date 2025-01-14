package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CorporateOrganisationType.CORP_TYPE)
class CorporateOrganisationType(code: String, description: String) : ReferenceCode(CORP_TYPE, code, description) {

  companion object {
    const val CORP_TYPE = "CORP_TYPE"
    fun pk(code: String): Pk = Pk(CORP_TYPE, code)
  }
}

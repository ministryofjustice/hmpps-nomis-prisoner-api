package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(HousingChangeReason.CHG_HOUS_RSN)
class HousingChangeReason(code: String, description: String) : ReferenceCode(CHG_HOUS_RSN, code, description) {
  companion object {
    const val CHG_HOUS_RSN = "CHG_HOUS_RSN"
    fun pk(code: String): Pk = Pk(CHG_HOUS_RSN, code)
  }
}

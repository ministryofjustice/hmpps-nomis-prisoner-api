package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(LivingUnitReason.LIV_UN_RSN)
class LivingUnitReason(code: String, description: String) : ReferenceCode(LIV_UN_RSN, code, description) {

  companion object {
    const val LIV_UN_RSN = "LIV_UN_RSN"
    fun pk(code: String): Pk = Pk(LIV_UN_RSN, code)
  }
}

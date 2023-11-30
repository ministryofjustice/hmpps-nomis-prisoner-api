package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SeriousnessLevelType.PSR_LEV_SER)
class SeriousnessLevelType(code: String, description: String) : ReferenceCode(PSR_LEV_SER, code, description) {
  companion object {
    const val PSR_LEV_SER = "PSR_LEV_SER"
    fun pk(code: String): Pk = Pk(PSR_LEV_SER, code)
  }
}

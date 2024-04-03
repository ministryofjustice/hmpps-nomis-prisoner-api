package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPAreaOfWork.CSIP_FUNC)
class CSIPAreaOfWork(code: String, description: String) : ReferenceCode(CSIP_FUNC, code, description) {
  companion object {
    const val CSIP_FUNC = "CSIP_FUNC"
    fun pk(code: String): Pk = Pk(CSIP_FUNC, code)
  }
}

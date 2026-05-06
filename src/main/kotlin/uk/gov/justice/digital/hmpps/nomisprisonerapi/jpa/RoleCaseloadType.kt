package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(RoleCaseloadType.CLOAD_TYPE)
class RoleCaseloadType(code: String, description: String) : ReferenceCode(CLOAD_TYPE, code, description) {
  companion object {
    const val CLOAD_TYPE = "CLOAD_TYPE"
    fun pk(code: String): Pk = Pk(CLOAD_TYPE, code)
    val APP = RoleCaseloadType("APP", "Application access caseload")
  }
}

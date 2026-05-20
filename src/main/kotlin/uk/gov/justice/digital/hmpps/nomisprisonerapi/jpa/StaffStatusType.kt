package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(StaffStatusType.STAFF_STATUS)
class StaffStatusType(code: String, description: String) : ReferenceCode(STAFF_STATUS, code, description) {

  companion object {
    const val STAFF_STATUS = "STAFF_STATUS"
    fun pk(code: String): Pk = Pk(STAFF_STATUS, code)
    val ACTIVE = StaffStatusType("ACTIVE", "Active")
  }
}

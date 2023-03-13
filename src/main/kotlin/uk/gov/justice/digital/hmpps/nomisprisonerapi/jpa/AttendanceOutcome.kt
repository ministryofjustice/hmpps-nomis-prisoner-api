package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AttendanceOutcome.ATTENDANCE_OUTCOME)
class AttendanceOutcome(code: String, description: String) : ReferenceCode(ATTENDANCE_OUTCOME, code, description) {

  companion object {
    const val ATTENDANCE_OUTCOME = "PS_PA_OC"
    fun pk(code: String): Pk = Pk(ATTENDANCE_OUTCOME, code)
  }
}

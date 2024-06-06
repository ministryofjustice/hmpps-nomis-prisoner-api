package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TaskSubType.TASK_SUBTYPE)
class TaskSubType(code: String, description: String) : ReferenceCode(TASK_SUBTYPE, code, description) {
  companion object {
    const val TASK_SUBTYPE = "TASK_SUBTYPE"

    fun pk(code: String): Pk = Pk(TASK_SUBTYPE, code)
  }
}

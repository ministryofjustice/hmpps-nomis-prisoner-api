package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(TaskType.TASK_TYPE)
class TaskType(code: String, description: String) : ReferenceCode(TASK_TYPE, code, description) {
  companion object {
    const val TASK_TYPE = "TASK_TYPE"

    fun pk(code: String): Pk = Pk(TASK_TYPE, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MarkType.MARK_TYPE_CODE)
class MarkType(code: String, description: String) : ReferenceCode(MARK_TYPE_CODE, code, description) {

  companion object {
    const val MARK_TYPE_CODE = "MARK_TYPE"
    fun pk(code: String): Pk = Pk(MARK_TYPE_CODE, code)
  }
}

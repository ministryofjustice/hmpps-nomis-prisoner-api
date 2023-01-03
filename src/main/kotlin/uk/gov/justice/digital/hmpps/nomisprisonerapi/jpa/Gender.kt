package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Gender.SEX)
class Gender(code: String, description: String) : ReferenceCode(SEX, code, description) {
  companion object {
    const val SEX = "SEX"
    val MALE = pk("M")
    val FEMALE = pk("F")
    fun pk(code: String): Pk = Pk(SEX, code)
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MilitaryBranch.MLTY_BRANCH)
class MilitaryBranch(code: String, description: String) : ReferenceCode(MLTY_BRANCH, code, description) {

  companion object {
    const val MLTY_BRANCH = "MLTY_BRANCH"
    fun pk(code: String): Pk = Pk(MLTY_BRANCH, code)
  }
}

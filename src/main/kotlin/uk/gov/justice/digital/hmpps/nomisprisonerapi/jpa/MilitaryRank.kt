package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MilitaryRank.MLTY_RANK)
class MilitaryRank(code: String, description: String) : ReferenceCode(MLTY_RANK, code, description) {

  companion object {
    const val MLTY_RANK = "MLTY_RANK"
    fun pk(code: String): Pk = Pk(MLTY_RANK, code)
  }
}

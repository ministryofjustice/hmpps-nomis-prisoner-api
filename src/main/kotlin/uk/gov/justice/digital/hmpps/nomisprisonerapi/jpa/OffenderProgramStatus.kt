package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus.Companion.OFFENDER_PROGRAM_STATUS

@Entity
@DiscriminatorValue(OFFENDER_PROGRAM_STATUS)
class OffenderProgramStatus(code: String, description: String) : ReferenceCode(OFFENDER_PROGRAM_STATUS, code, description) {

  companion object {
    const val OFFENDER_PROGRAM_STATUS = "OFF_PRG_STS"
    fun pk(code: String): Pk = Pk(OFFENDER_PROGRAM_STATUS, code)
  }
}

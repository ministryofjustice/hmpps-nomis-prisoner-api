package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService

class ProgramServiceBuilder(
  var programCode: String = "INTTEST",
  var programId: Long = 20,
  var description: String = "test program",
  var active: Boolean = true,
) {
  fun build() = ProgramService(
    programCode = programCode,
    programId = programId,
    description = description,
    active = active
  )
}

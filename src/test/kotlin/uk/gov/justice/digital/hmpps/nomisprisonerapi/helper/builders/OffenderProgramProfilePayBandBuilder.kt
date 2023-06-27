package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBandId
import java.time.LocalDate

class OffenderProgramProfilePayBandBuilder(
  val repository: Repository,
  val startDate: String,
  val endDate: String?,
  val payBandCode: String,
) : CourseAllocationPayBandDsl {
  fun build(offenderProgramProfile: OffenderProgramProfile) =
    OffenderProgramProfilePayBand(
      id = OffenderProgramProfilePayBandId(
        offenderProgramProfile = offenderProgramProfile,
        startDate = LocalDate.parse(startDate),
      ),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      payBand = repository.lookupPayBandCode(payBandCode),
    )
}

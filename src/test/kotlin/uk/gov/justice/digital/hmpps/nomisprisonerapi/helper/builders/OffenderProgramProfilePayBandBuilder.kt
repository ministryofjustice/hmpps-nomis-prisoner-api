package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import java.time.LocalDate

@Component
class OffenderProgramProfilePayBandBuilderFactory(private val repository: Repository) {
  fun builder(
    startDate: String = "2022-10-31",
    endDate: String? = null,
    payBandCode: String = "5",
  ): OffenderProgramProfilePayBandBuilder =
    OffenderProgramProfilePayBandBuilder(
      repository,
      startDate,
      endDate,
      payBandCode,
    )
}

class OffenderProgramProfilePayBandBuilder(
  val repository: Repository,
  val startDate: String,
  val endDate: String?,
  val payBandCode: String,
) {
  fun build(offenderProgramProfile: OffenderProgramProfile) =
    OffenderProgramProfilePayBand(
      offenderProgramProfile = offenderProgramProfile,
      startDate = LocalDate.parse(startDate),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      payBand = repository.lookupPayBandCode(payBandCode),
    )
}

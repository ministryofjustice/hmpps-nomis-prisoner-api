package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileId
import java.time.LocalDate

@DslMarker
annotation class OffenderProfileDslMarker

@NomisDataDslMarker
interface OffenderProfileDsl

@Component
class OffenderProfileBuilderFactory {
  fun builder() = OffenderProfileBuilder()
}

class OffenderProfileBuilder : OffenderProfileDsl {
  private lateinit var offenderProfile: OffenderProfile

  fun build(
    offenderBooking: OffenderBooking,
    checkDate: LocalDate,
    sequence: Long,
  ): OffenderProfile = OffenderProfile(
    id = OffenderProfileId(
      offenderBooking = offenderBooking,
      sequence = sequence,
    ),
    checkDate = checkDate,
  ).also {
    offenderProfile = it
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAttendee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview

@DslMarker
annotation class CSIPAttendeeDslMarker

@NomisDataDslMarker
interface CSIPAttendeeDsl

@Component
class CSIPAttendeeBuilderFactory {
  fun builder() = CSIPAttendeeBuilder()
}

class CSIPAttendeeBuilder :
  CSIPAttendeeDsl {

  fun build(
    csipReview: CSIPReview,
    name: String?,
    role: String?,
    attended: Boolean,
    contribution: String?,
  ): CSIPAttendee =
    CSIPAttendee(
      csipReview = csipReview,
      name = name,
      role = role,
      attended = attended,
      contribution = contribution,
    )
}

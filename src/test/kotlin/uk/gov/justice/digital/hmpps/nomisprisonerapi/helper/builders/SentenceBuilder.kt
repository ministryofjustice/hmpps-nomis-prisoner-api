package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import java.time.LocalDate

class SentenceBuilder(
  var calculationType: String = "ADIMP_ORA",
  var category: String = "2003",
  var startDate: LocalDate = LocalDate.now(),
  var status: String = "I",
) {
  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long = 1,
    calculationType: SentenceCalculationType,
  ): OffenderSentence =
    OffenderSentence(
      id = SentenceId(offenderBooking = offenderBooking, sequence = sequence),
      status = status,
      startDate = startDate,
      calculationType = calculationType
    )
}

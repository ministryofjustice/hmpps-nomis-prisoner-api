package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import java.time.LocalDate

class KeyDateAdjustmentBuilder(
  var adjustmentTypeCode: String = "ADA",
  var adjustmentDate: LocalDate = LocalDate.now(),
  var adjustmentNumberOfDays: Long = 10
) {
  fun build(offenderBooking: OffenderBooking, adjustmentType: SentenceAdjustment): OffenderKeyDateAdjustment =
    OffenderKeyDateAdjustment(
      offenderBooking = offenderBooking,
      sentenceAdjustment = adjustmentType,
      adjustmentDate = adjustmentDate,
      adjustmentNumberOfDays = adjustmentNumberOfDays,
    )
}

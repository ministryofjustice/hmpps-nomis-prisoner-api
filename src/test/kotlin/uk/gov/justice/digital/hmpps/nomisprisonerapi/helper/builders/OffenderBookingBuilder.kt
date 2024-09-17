package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

class OffenderBookingBuilder(
  var bookingBeginDate: LocalDateTime = LocalDateTime.now(),
  var active: Boolean = true,
  var inOutStatus: String = "IN",
  var youthAdultCode: String = "N",
  var agencyLocationId: String = "BXI",
  var incentives: List<LegacyIncentiveBuilder> = emptyList(),
  var sentences: List<LegacySentenceBuilder> = emptyList(),
  var keyDateAdjustments: List<LegacyKeyDateAdjustmentBuilder> = emptyList(),
  val repository: Repository? = null,
) {
  fun build(offender: Offender, bookingSequence: Int, agencyLocation: AgencyLocation): OffenderBooking =
    OffenderBooking(
      offender = offender,
      rootOffender = offender.rootOffender,
      bookingBeginDate = bookingBeginDate,
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
      bookingSequence = bookingSequence,
      createLocation = agencyLocation,
      location = agencyLocation,
    ).apply {
      offender.getAllBookings()?.add(this)
    }

  fun withSentences(vararg sentenceBuilder: LegacySentenceBuilder): OffenderBookingBuilder {
    this.sentences = arrayOf(*sentenceBuilder).asList()
    return this
  }
  fun withKeyDateAdjustments(vararg keyDateAdjustmentBuilder: LegacyKeyDateAdjustmentBuilder): OffenderBookingBuilder {
    this.keyDateAdjustments = arrayOf(*keyDateAdjustmentBuilder).asList()
    return this
  }
}

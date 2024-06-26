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
  var visitBalanceBuilder: VisitBalanceBuilder? = null,
  var agencyLocationId: String = "BXI",
  var contacts: List<OffenderContactBuilder> = emptyList(),
  var visits: List<VisitBuilder> = emptyList(),
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

  fun withVisitBalance(visitBalanceBuilder: VisitBalanceBuilder = VisitBalanceBuilder()): OffenderBookingBuilder {
    this.visitBalanceBuilder = visitBalanceBuilder
    return this
  }

  fun withContacts(vararg contactBuilder: OffenderContactBuilder): OffenderBookingBuilder {
    this.contacts = arrayOf(*contactBuilder).asList()
    return this
  }

  fun withVisits(vararg visitBuilder: VisitBuilder): OffenderBookingBuilder {
    this.visits = arrayOf(*visitBuilder).asList()
    return this
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

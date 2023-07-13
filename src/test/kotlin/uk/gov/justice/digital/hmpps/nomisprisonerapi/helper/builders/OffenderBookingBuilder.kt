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
  var sentences: List<SentenceBuilder> = emptyList(),
  var keyDateAdjustments: List<KeyDateAdjustmentBuilder> = emptyList(),
  val repository: Repository? = null,
) {
  fun build(offender: Offender, bookingSequence: Int, agencyLocation: AgencyLocation): OffenderBooking =
    OffenderBooking(
      offender = offender,
      rootOffender = offender,
      bookingBeginDate = bookingBeginDate,
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
      bookingSequence = bookingSequence,
      createLocation = agencyLocation,
      location = agencyLocation,
    )

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
  fun withIncentives(vararg incentiveBuilder: LegacyIncentiveBuilder): OffenderBookingBuilder {
    this.incentives = arrayOf(*incentiveBuilder).asList()
    return this
  }
  fun withSentences(vararg sentenceBuilder: SentenceBuilder): OffenderBookingBuilder {
    this.sentences = arrayOf(*sentenceBuilder).asList()
    return this
  }
  fun withKeyDateAdjustments(vararg keyDateAdjustmentBuilder: KeyDateAdjustmentBuilder): OffenderBookingBuilder {
    this.keyDateAdjustments = arrayOf(*keyDateAdjustmentBuilder).asList()
    return this
  }
}

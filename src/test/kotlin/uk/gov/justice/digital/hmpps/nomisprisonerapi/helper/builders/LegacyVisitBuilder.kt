package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDateTime

class LegacyVisitBuilder(
  var visitTypeCode: String = "SCON",
  var visitStatusCode: String = "SCH",
  var startDateTimeString: String = "2022-01-01T12:05",
  var endDateTimeString: String = "2022-01-01T13:05",
  var agyLocId: String = "MDI",
  var agencyInternalLocationDescription: String? = "MDI-1-1-001",
  var visitors: List<LegacyVisitVisitorBuilder> = emptyList(),
  var visitOutcome: LegacyVisitOutcomeBuilder = LegacyVisitOutcomeBuilder(),
) {
  fun build(
    offenderBooking: OffenderBooking,
    visitType: VisitType,
    visitStatus: VisitStatus,
    agencyLocation: AgencyLocation,
    agencyInternalLocation: AgencyInternalLocation?,
  ): Visit {
    val startDateTime = LocalDateTime.parse(startDateTimeString)
    return Visit(
      offenderBooking = offenderBooking,
      startDateTime = startDateTime,
      endDateTime = LocalDateTime.parse(endDateTimeString),
      visitType = visitType,
      visitStatus = visitStatus,
      location = agencyLocation,
      visitDate = startDateTime.toLocalDate(),
      agencyInternalLocation = agencyInternalLocation,
    )
  }

  fun withVisitors(vararg visitVisitorBuilders: LegacyVisitVisitorBuilder): LegacyVisitBuilder {
    this.visitors = arrayOf(*visitVisitorBuilders).asList()
    return this
  }
}

class LegacyVisitVisitorBuilder(
  val person: Person,
  val leadVisitor: Boolean = false,
) {
  fun build(person: Person, leadVisitor: Boolean, visit: Visit): VisitVisitor =
    VisitVisitor(
      person = person,
      visit = visit,
      groupLeader = leadVisitor,
    )
}

class LegacyVisitOutcomeBuilder(
  val outcomeCode: String? = null,
) {
  fun build(visit: Visit): VisitVisitor =
    VisitVisitor(
      person = null,
      visit = visit,
      groupLeader = false,
      outcomeReasonCode = outcomeCode,
    )
}

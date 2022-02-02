package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDate
import java.time.LocalDateTime

class VisitBuilder(
  var visitTypeCode: String = "SCON",
  var visitStatusCode: String = "SCH",
  var agyLocId: String = "MDI",
  var visitors: List<VisitVisitorBuilder> = emptyList(),
) {
  fun build(
    offenderBooking: OffenderBooking,
    visitType: VisitType,
    visitStatus: VisitStatus,
    agencyLocation: AgencyLocation
  ): Visit =
    Visit(
      offenderBooking = offenderBooking,
      startDateTime = LocalDateTime.parse("2022-01-01T12:05"),
      endDateTime = LocalDateTime.parse("2022-01-01T13:05"),
      visitType = visitType,
      visitStatus = visitStatus,
      location = agencyLocation,
      visitDate = LocalDate.parse("2022-01-01"),
    )

  fun withVisitors(vararg visitVisitorBuilders: VisitVisitorBuilder): VisitBuilder {
    this.visitors = arrayOf(*visitVisitorBuilders).asList()
    return this
  }
}

class VisitVisitorBuilder(
  val person: Person,
  val leadVisitor: Boolean = false
) {
  fun build(person: Person, leadVisitor: Boolean, visit: Visit): VisitVisitor =
    VisitVisitor(
      person = person, visit = visit, groupLeader = leadVisitor
    )
}

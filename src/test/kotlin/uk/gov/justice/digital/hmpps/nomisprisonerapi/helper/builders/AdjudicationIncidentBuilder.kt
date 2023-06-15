package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationIncidentBuilder(
  var incidentDetails: String = "Big fight",
  var reportedDateTime: LocalDateTime = LocalDateTime.now(),
  var reportedDate: LocalDate = LocalDate.now(),
  var incidentDateTime: LocalDateTime = LocalDateTime.now(),
  var incidentDate: LocalDate = LocalDate.now(),
  var parties: List<AdjudicationPartyBuilder> = listOf(),
  var prisonId: String = "MDI",
  val agencyInternalLocationId: Long = -41,
  val reportingStaff: Staff,
) {

  fun build(
    agencyInternalLocation: AgencyInternalLocation,
    incidentType: AdjudicationIncidentType,
    prison: AgencyLocation,
    reportingStaff: Staff,
  ): AdjudicationIncident =
    AdjudicationIncident(
      reportingStaff = reportingStaff,
      reportedDateTime = reportedDateTime,
      reportedDate = reportedDate,
      incidentDateTime = incidentDateTime,
      incidentDate = incidentDate,
      agencyInternalLocation = agencyInternalLocation,
      incidentType = incidentType,
      prison = prison,
      incidentDetails = incidentDetails,
    )

  fun withParties(vararg parties: AdjudicationPartyBuilder): AdjudicationIncidentBuilder {
    this.parties = arrayOf(*parties).asList()
    return this
  }
}

class AdjudicationPartyBuilder(
  var adjudicationNumber: Long = 1224,
) {

  fun build(
    incident: AdjudicationIncident,
    offenderBooking: OffenderBooking,
    actionDecision: IncidentDecisionAction,
    index: Int,
  ): AdjudicationIncidentParty =
    AdjudicationIncidentParty(
      id = AdjudicationIncidentPartyId(incident.id, index),
      offenderBooking = offenderBooking,
      adjudicationNumber = adjudicationNumber,
      incidentRole = "S",
      incident = incident,
      actionDecision = actionDecision,
    )
}

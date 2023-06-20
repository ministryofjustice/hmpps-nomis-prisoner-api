package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepairId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationRepairType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationIncidentBuilder(
  var incidentDetails: String = "Big fight",
  var reportedDateTime: LocalDateTime = LocalDateTime.now(),
  var reportedDate: LocalDate = LocalDate.now(),
  var incidentDateTime: LocalDateTime = LocalDateTime.now(),
  var incidentDate: LocalDate = LocalDate.now(),
  var parties: List<AdjudicationPartyBuilder> = listOf(),
  var repairs: List<AdjudicationRepairBuilder> = listOf(),
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
  fun withRepairs(vararg repairs: AdjudicationRepairBuilder): AdjudicationIncidentBuilder {
    this.repairs = arrayOf(*repairs).asList()
    return this
  }
}

class AdjudicationPartyBuilder(
  var adjudicationNumber: Long = 1224,
  var comment: String = "party comment",
  var charges: List<AdjudicationChargeBuilder> = listOf(),
  var partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
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
      partyAddedDate = partyAddedDate,
      comment = comment,
    )

  fun withCharges(vararg chargesBuilder: AdjudicationChargeBuilder): AdjudicationPartyBuilder {
    this.charges = arrayOf(*chargesBuilder).asList()
    return this
  }
}

class AdjudicationChargeBuilder(
  var offenceCode: String = "51:1B",
  var guiltyEvidence: String? = null,
  var reportDetail: String? = null,
) {

  fun build(
    incidentParty: AdjudicationIncidentParty,
    chargeSequence: Int,
    offence: AdjudicationIncidentOffence,
  ): AdjudicationIncidentCharge =
    AdjudicationIncidentCharge(
      id = AdjudicationIncidentChargeId(incidentParty.id.agencyIncidentId, chargeSequence),
      incident = incidentParty.incident,
      partySequence = incidentParty.id.partySequence,
      incidentParty = incidentParty,
      offence = offence,
      guiltyEvidence = guiltyEvidence,
      reportDetails = reportDetail,
    )
}

class AdjudicationRepairBuilder(
  var repairType: String = "CLEA",
  var comment: String? = null,
  var repairCost: BigDecimal? = null,
) {

  fun build(
    incident: AdjudicationIncident,
    repairSequence: Int,
    type: AdjudicationRepairType,
  ): AdjudicationIncidentRepair =
    AdjudicationIncidentRepair(
      id = AdjudicationIncidentRepairId(incident.id, repairSequence),
      incident = incident,
      comment = comment,
      repairCost = repairCost,
      type = type,
    )
}

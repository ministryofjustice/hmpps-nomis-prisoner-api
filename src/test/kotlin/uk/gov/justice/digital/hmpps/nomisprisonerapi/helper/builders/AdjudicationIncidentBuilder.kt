package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepairId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationPleaFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationRepairType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.suspectRole
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationIncidentBuilder(
  private var incidentDetails: String,
  private var reportedDateTime: LocalDateTime,
  private var reportedDate: LocalDate,
  private var incidentDateTime: LocalDateTime,
  private var incidentDate: LocalDate,
  var parties: List<AdjudicationPartyBuilder> = listOf(),
  var repairs: List<AdjudicationRepairBuilder> = listOf(),
  var prisonId: String,
  val agencyInternalLocationId: Long,
  val reportingStaff: Staff,
) : AdjudicationIncidentDsl {

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

  override fun repair(
    repairType: String,
    comment: String?,
    repairCost: BigDecimal?,
    dsl: AdjudicationRepairDsl.() -> Unit,
  ) {
    this.repairs += AdjudicationRepairBuilder(repairType, comment, repairCost).apply(dsl)
  }

  override fun party(
    comment: String,
    role: PartyRole,
    partyAddedDate: LocalDate,
    offenderBooking: OffenderBooking?,
    staff: Staff?,
    adjudicationNumber: Long?,
    actionDecision: String,
    dsl: AdjudicationPartyDsl.() -> Unit,
  ) {
    this.parties += AdjudicationPartyBuilder(
      comment = comment,
      offenderBooking = offenderBooking,
      staff = staff,
      partyAddedDate = partyAddedDate,
      incidentRole = role.code,
      actionDecision = actionDecision,
      adjudicationNumber = adjudicationNumber,
    ).apply(dsl)
  }
}

class AdjudicationPartyBuilder(
  private var adjudicationNumber: Long? = null,
  private var comment: String,
  private var offenderBooking: OffenderBooking?,
  private var staff: Staff?,
  private var incidentRole: String = suspectRole,
  var actionDecision: String = IncidentDecisionAction.NO_FURTHER_ACTION_CODE,
  private var partyAddedDate: LocalDate,
  var charges: List<AdjudicationChargeBuilder> = listOf(),
  var hearings: List<AdjudicationHearingBuilder> = listOf(),
  var investigations: List<AdjudicationInvestigationBuilder> = listOf(),
) : AdjudicationPartyDsl {

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

  fun build(
    incident: AdjudicationIncident,
    actionDecision: IncidentDecisionAction,
    index: Int,
  ): AdjudicationIncidentParty =
    AdjudicationIncidentParty(
      id = AdjudicationIncidentPartyId(incident.id, index),
      offenderBooking = offenderBooking,
      staff = staff,
      adjudicationNumber = adjudicationNumber,
      incidentRole = incidentRole,
      incident = incident,
      actionDecision = actionDecision,
      partyAddedDate = partyAddedDate,
      comment = comment,
    )

  override fun investigation(
    investigator: Staff,
    comment: String?,
    assignedDate: LocalDate,
    dsl: AdjudicationInvestigationDsl.() -> Unit,
  ) {
    this.investigations += AdjudicationInvestigationBuilder(
      investigator = investigator,
      comment = comment,
      assignedDate = assignedDate,
    ).apply(dsl)
  }

  override fun charge(
    offenceCode: String,
    guiltyEvidence: String?,
    reportDetail: String?,
    dsl: AdjudicationChargeDsl.() -> Unit,
  ) {
    this.charges += AdjudicationChargeBuilder(offenceCode, guiltyEvidence, reportDetail).apply(dsl)
  }

  override fun hearing(
    internalLocationId: Long?,
    scheduleDate: LocalDate?,
    scheduleTime: LocalDateTime?,
    hearingDate: LocalDate?,
    hearingTime: LocalDateTime?,
    hearingStaffId: Long?,
    dsl: AdjudicationHearingDsl.() -> Unit,
  ) {
    this.hearings += AdjudicationHearingBuilder(
      agencyInternalLocationId = internalLocationId,
      scheduledDate = scheduleDate,
      scheduledDateTime = scheduleTime,
      hearingDate = hearingDate,
      hearingDateTime = hearingTime,
      hearingStaffId = hearingStaffId,
    ).apply(dsl)
  }
}

class AdjudicationChargeBuilder(
  var offenceCode: String,
  private var guiltyEvidence: String?,
  private var reportDetail: String?,
) : AdjudicationChargeDsl {

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
  var repairType: String,
  private var comment: String?,
  private var repairCost: BigDecimal?,
) : AdjudicationRepairDsl {

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

class AdjudicationInvestigationBuilder(
  private var investigator: Staff,
  private var comment: String? = null,
  private var assignedDate: LocalDate = LocalDate.now(),
  var evidence: List<AdjudicationEvidenceBuilder> = listOf(),
) : AdjudicationInvestigationDsl {

  fun build(incidentParty: AdjudicationIncidentParty): AdjudicationInvestigation =
    AdjudicationInvestigation(
      investigator = investigator,
      assignedDate = assignedDate,
      comment = comment,
      incidentParty = incidentParty,
    )

  override fun evidence(detail: String, type: String, date: LocalDate, dsl: AdjudicationEvidenceDsl.() -> Unit) {
    this.evidence += AdjudicationEvidenceBuilder(detail = detail, type = type, date = date).apply(dsl)
  }
}

class AdjudicationEvidenceBuilder(
  private var detail: String = "Knife found",
  var type: String = "WEAP",
  private var date: LocalDate = LocalDate.now(),
) : AdjudicationEvidenceDsl {
  fun build(investigation: AdjudicationInvestigation, type: AdjudicationEvidenceType): AdjudicationEvidence =
    AdjudicationEvidence(
      statementDate = date,
      statementDetail = detail,
      statementType = type,
      investigation = investigation,
    )
}

class AdjudicationHearingBuilder(
  private var hearingDate: LocalDate? = LocalDate.now(),
  private var hearingDateTime: LocalDateTime? = LocalDateTime.now(),
  private var scheduledDate: LocalDate? = LocalDate.now(),
  private var scheduledDateTime: LocalDateTime? = LocalDateTime.now(),
  var hearingStaffId: Long? = null,
  var eventStatusCode: String = "SCH",
  var hearingTypeCode: String = AdjudicationHearingType.GOVERNORS_HEARING,
  private var comment: String = "Hearing comment",
  private var representativeText: String = "rep text",
  var agencyInternalLocationId: Long?,
  var results: List<AdjudicationHearingResultBuilder> = listOf(),
) : AdjudicationHearingDsl {

  fun build(
    incidentParty: AdjudicationIncidentParty,
    agencyInternalLocation: AgencyInternalLocation?,
    hearingType: AdjudicationHearingType?,
    hearingStaff: Staff?,
    eventStatus: EventStatus?,
    eventId: Long? = 1,
    results: MutableList<AdjudicationHearingResult> = mutableListOf(),
  ): AdjudicationHearing =
    AdjudicationHearing(
      hearingParty = incidentParty,
      adjudicationNumber = incidentParty.adjudicationNumber!!,
      hearingDate = hearingDate,
      hearingDateTime = hearingDateTime,
      scheduleDate = scheduledDate,
      scheduleDateTime = scheduledDateTime,
      hearingStaff = hearingStaff,
      hearingType = hearingType,
      agencyInternalLocation = agencyInternalLocation,
      eventStatus = eventStatus,
      eventId = eventId,
      comment = comment,
      representativeText = representativeText,
      hearingResults = results,
    )

  override fun result(chargeSequence: Int, pleaFindingCode: String, findingCode: String, dsl: AdjudicationHearingResultDsl.() -> Unit) {
    this.results += AdjudicationHearingResultBuilder(
      pleaFindingCode,
      findingCode = findingCode,
      chargeSequence = chargeSequence,
    ).apply(dsl)
  }
}

class AdjudicationHearingResultBuilder(
  var pleaFindingCode: String,
  var findingCode: String,
  var chargeSequence: Int,
) : AdjudicationHearingResultDsl {
  fun build(
    hearing: AdjudicationHearing,
    index: Int,
    charge: AdjudicationIncidentCharge,
    pleaFindingType: AdjudicationPleaFindingType?,
    findingType: AdjudicationFindingType,
  ): AdjudicationHearingResult =
    AdjudicationHearingResult(
      id = AdjudicationHearingResultId(hearing.id, index),
      chargeSequence = chargeSequence,
      incidentId = charge.incident.id,
      hearing = hearing,
      offence = charge.offence,
      incidentCharge = charge,
      pleaFindingType = pleaFindingType,
      findingType = findingType,
      pleaFindingCode = pleaFindingCode,
    )
}

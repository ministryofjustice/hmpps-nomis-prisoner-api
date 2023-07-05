package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAwardId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepairId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationIncidentBuilder(
  private val incidentDetails: String,
  private val reportedDateTime: LocalDateTime,
  private val reportedDate: LocalDate,
  private val incidentDateTime: LocalDateTime,
  private val incidentDate: LocalDate,
  private val prisonId: String,
  private val agencyInternalLocationId: Long,
  private val reportingStaff: Staff,
  var parties: List<AdjudicationPartyBuilder> = listOf(),
  var repairs: List<AdjudicationRepairBuilder> = listOf(),
) : AdjudicationIncidentDsl {

  fun build(
    repository: Repository,
  ): AdjudicationIncident =
    AdjudicationIncident(
      reportingStaff = reportingStaff,
      reportedDateTime = reportedDateTime,
      reportedDate = reportedDate,
      incidentDateTime = incidentDateTime,
      incidentDate = incidentDate,
      agencyInternalLocation = repository.lookupAgencyInternalLocation(agencyInternalLocationId)!!,
      incidentType = repository.lookupIncidentType(),
      prison = repository.lookupAgency(prisonId),
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
  private val adjudicationNumber: Long? = null,
  private val comment: String,
  private val offenderBooking: OffenderBooking?,
  private val staff: Staff?,
  private val incidentRole: String,
  private val actionDecision: String,
  private val partyAddedDate: LocalDate,
  var charges: List<AdjudicationChargeBuilder> = listOf(),
  var hearings: List<AdjudicationHearingBuilder> = listOf(),
  var investigations: List<AdjudicationInvestigationBuilder> = listOf(),
) : AdjudicationPartyDsl {

  fun build(
    repository: Repository,
    incident: AdjudicationIncident,
    offenderBooking: OffenderBooking,
    index: Int,
  ): AdjudicationIncidentParty =
    AdjudicationIncidentParty(
      id = AdjudicationIncidentPartyId(incident.id, index),
      offenderBooking = offenderBooking,
      adjudicationNumber = adjudicationNumber,
      incidentRole = "S",
      incident = incident,
      actionDecision = repository.lookupActionDecision(actionDecision),
      partyAddedDate = partyAddedDate,
      comment = comment,
    )

  fun build(
    repository: Repository,
    incident: AdjudicationIncident,
    index: Int,
  ): AdjudicationIncidentParty =
    AdjudicationIncidentParty(
      id = AdjudicationIncidentPartyId(incident.id, index),
      offenderBooking = offenderBooking,
      staff = staff,
      adjudicationNumber = adjudicationNumber,
      incidentRole = incidentRole,
      incident = incident,
      actionDecision = repository.lookupActionDecision(actionDecision),
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
    ref: DataRef<AdjudicationIncidentCharge>?,
    dsl: AdjudicationChargeDsl.() -> Unit,
  ) {
    this.charges += AdjudicationChargeBuilder(offenceCode, guiltyEvidence, reportDetail, ref).apply(dsl)
  }

  override fun hearing(
    internalLocationId: Long?,
    scheduleDate: LocalDate?,
    scheduleTime: LocalDateTime?,
    hearingDate: LocalDate?,
    hearingTime: LocalDateTime?,
    hearingStaff: Staff?,
    hearingTypeCode: String,
    eventStatusCode: String,
    comment: String,
    representativeText: String,
    dsl: AdjudicationHearingDsl.() -> Unit,
  ) {
    this.hearings += AdjudicationHearingBuilder(
      agencyInternalLocationId = internalLocationId,
      scheduledDate = scheduleDate,
      scheduledDateTime = scheduleTime,
      hearingDate = hearingDate,
      hearingDateTime = hearingTime,
      hearingStaff = hearingStaff,
      comment = comment,
      representativeText = representativeText,
      eventStatusCode = eventStatusCode,
      hearingTypeCode = hearingTypeCode,
    ).apply(dsl)
  }
}

class AdjudicationChargeBuilder(
  private val offenceCode: String,
  private val guiltyEvidence: String?,
  private val reportDetail: String?,
  private val ref: DataRef<AdjudicationIncidentCharge>?,
) : AdjudicationChargeDsl {

  fun build(
    repository: Repository,
    incidentParty: AdjudicationIncidentParty,
    chargeSequence: Int,
  ): AdjudicationIncidentCharge =
    AdjudicationIncidentCharge(
      id = AdjudicationIncidentChargeId(incidentParty.id.agencyIncidentId, chargeSequence),
      incident = incidentParty.incident,
      partySequence = incidentParty.id.partySequence,
      incidentParty = incidentParty,
      offence = repository.lookupAdjudicationOffence(offenceCode),
      guiltyEvidence = guiltyEvidence,
      reportDetails = reportDetail,
    ).also { ref?.set(it) }
}

class AdjudicationRepairBuilder(
  private val repairType: String,
  private val comment: String?,
  private val repairCost: BigDecimal?,
) : AdjudicationRepairDsl {

  fun build(
    repository: Repository,
    incident: AdjudicationIncident,
    repairSequence: Int,
  ): AdjudicationIncidentRepair =
    AdjudicationIncidentRepair(
      id = AdjudicationIncidentRepairId(incident.id, repairSequence),
      incident = incident,
      comment = comment,
      repairCost = repairCost,
      type = repository.lookupRepairType(repairType),
    )
}

class AdjudicationInvestigationBuilder(
  private val investigator: Staff,
  private val comment: String? = null,
  private val assignedDate: LocalDate = LocalDate.now(),
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
  private val detail: String,
  private val type: String,
  private val date: LocalDate,
) : AdjudicationEvidenceDsl {
  fun build(repository: Repository, investigation: AdjudicationInvestigation): AdjudicationEvidence =
    AdjudicationEvidence(
      statementDate = date,
      statementDetail = detail,
      statementType = repository.lookupAdjudicationEvidenceType(type),
      investigation = investigation,
    )
}

class AdjudicationHearingBuilder(
  private val hearingDate: LocalDate?,
  private val hearingDateTime: LocalDateTime?,
  private val scheduledDate: LocalDate?,
  private val scheduledDateTime: LocalDateTime?,
  private val hearingStaff: Staff? = null,
  private val comment: String,
  private val representativeText: String,
  private val agencyInternalLocationId: Long?,
  private val hearingTypeCode: String,
  private val eventStatusCode: String,
  var results: List<AdjudicationHearingResultBuilder> = listOf(),
) : AdjudicationHearingDsl {

  fun build(
    repository: Repository,
    incidentParty: AdjudicationIncidentParty,
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
      hearingType = repository.lookupHearingType(hearingTypeCode),
      agencyInternalLocation = repository.lookupAgencyInternalLocation(agencyInternalLocationId!!),
      eventStatus = repository.lookupEventStatusCode(eventStatusCode),
      eventId = eventId,
      comment = comment,
      representativeText = representativeText,
      hearingResults = results,
    )

  override fun result(
    chargeRef: DataRef<AdjudicationIncidentCharge>,
    pleaFindingCode: String,
    findingCode: String,
    dsl: AdjudicationHearingResultDsl.() -> Unit,
  ) {
    this.results += AdjudicationHearingResultBuilder(
      pleaFindingCode,
      findingCode = findingCode,
      chargeRef = chargeRef,
    ).apply(dsl)
  }
}

class AdjudicationHearingResultBuilder(
  private val pleaFindingCode: String,
  private val findingCode: String,
  private val chargeRef: DataRef<AdjudicationIncidentCharge>,
  var awards: List<AdjudicationHearingResultAwardBuilder> = listOf(),
) : AdjudicationHearingResultDsl {
  fun build(
    repository: Repository,
    hearing: AdjudicationHearing,
    index: Int,
  ): AdjudicationHearingResult =
    AdjudicationHearingResult(
      id = AdjudicationHearingResultId(hearing.id, index),
      chargeSequence = chargeRef.value().id.chargeSequence,
      incident = chargeRef.value().incident,
      hearing = hearing,
      offence = chargeRef.value().offence,
      incidentCharge = chargeRef.value(),
      pleaFindingType = repository.lookupHearingResultPleaType(pleaFindingCode),
      findingType = repository.lookupHearingResultFindingType(findingCode),
      pleaFindingCode = pleaFindingCode,
      resultAwards = mutableListOf(),
    )

  override fun award(
    statusCode: String,
    sanctionDays: Int?,
    sanctionMonths: Int?,
    compensationAmount: BigDecimal?,
    sanctionCode: String,
    comment: String?,
    effectiveDate: LocalDate,
    statusDate: LocalDate?,
    consecutiveSanctionSeq: Int?,
    dsl: AdjudicationHearingResultAwardDsl.() -> Unit,
  ) {
    this.awards += AdjudicationHearingResultAwardBuilder(
      statusCode = statusCode,
      sanctionDays = sanctionDays,
      sanctionMonths = sanctionMonths,
      compensationAmount = compensationAmount,
      sanctionCode = sanctionCode,
      effectiveDate = effectiveDate,
      statusDate = statusDate,
      consecutiveSanctionIndex = consecutiveSanctionSeq,
      comment = comment,
    ).apply(dsl)
  }
}

class AdjudicationHearingResultAwardBuilder(
  private val sanctionCode: String,
  private val effectiveDate: LocalDate,
  private val statusDate: LocalDate?,
  private val statusCode: String,
  private val comment: String?,
  private val sanctionDays: Int?,
  private val sanctionMonths: Int?,
  private val compensationAmount: BigDecimal?,
  var consecutiveSanctionIndex: Int? = null,
) : AdjudicationHearingResultAwardDsl {
  fun build(
    repository: Repository,
    result: AdjudicationHearingResult,
    party: AdjudicationIncidentParty,
    sanctionIndex: Int,
    consecutiveHearingResultAward: AdjudicationHearingResultAward? = null,
  ): AdjudicationHearingResultAward =
    AdjudicationHearingResultAward(
      id = AdjudicationHearingResultAwardId(party.offenderBooking!!.bookingId, sanctionIndex),
      hearingResult = result,
      sanctionStatus = repository.lookupSanctionStatus(statusCode),
      sanctionDays = sanctionDays,
      sanctionMonths = sanctionMonths,
      compensationAmount = compensationAmount,
      incidentParty = party,
      sanctionType = repository.lookupSanctionType(sanctionCode),
      sanctionCode = sanctionCode,
      consecutiveHearingResultAward = consecutiveHearingResultAward,
      effectiveDate = effectiveDate,
      statusDate = statusDate,
      comment = comment,
    )
}

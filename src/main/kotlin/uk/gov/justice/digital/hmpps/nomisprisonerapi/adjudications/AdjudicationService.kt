package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepairId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationRepairType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.PLACED_ON_REPORT_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.findAdjudication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isInvolvedForForce
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isInvolvedForOtherReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isReportingOfficer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isSuspect
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isVictim
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isWitness
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.prisonerOnReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.prisonerParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentOffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentPartyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.AdjudicationSpecification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.staffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.suspectRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.victimRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.witnessRole

@Service
@Transactional
class AdjudicationService(
  private val adjudicationIncidentPartyRepository: AdjudicationIncidentPartyRepository,
  private val adjudicationIncidentRepository: AdjudicationIncidentRepository,
  private val adjudicationHearingRepository: AdjudicationHearingRepository,
  private val offenderRepository: OffenderRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val adjudicationIncidentOffenceRepository: AdjudicationIncidentOffenceRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val adjudicationIncidentTypeRepository: ReferenceCodeRepository<AdjudicationIncidentType>,
  private val incidentDecisionActionRepository: ReferenceCodeRepository<IncidentDecisionAction>,
  private val evidenceTypeRepository: ReferenceCodeRepository<AdjudicationEvidenceType>,
  private val repairTypeRepository: ReferenceCodeRepository<AdjudicationRepairType>,
) {

  fun getAdjudication(adjudicationNumber: Long): AdjudicationResponse =
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.let {
      val hearings = adjudicationHearingRepository.findByAdjudicationNumber(adjudicationNumber)
      return mapAdjudication(it, hearings)
    }
      ?: throw NotFoundException("Adjudication not found")

  private fun mapAdjudication(
    adjudication: AdjudicationIncidentParty,
    hearings: List<AdjudicationHearing> = emptyList(),
  ): AdjudicationResponse {
    return AdjudicationResponse(
      adjudicationNumber = adjudication.adjudicationNumber,
      adjudicationSequence = adjudication.id.partySequence,
      offenderNo = adjudication.prisonerOnReport().offender.nomsId,
      bookingId = adjudication.prisonerOnReport().bookingId,
      partyAddedDate = adjudication.partyAddedDate,
      comment = adjudication.comment,
      incident = AdjudicationIncident(
        adjudicationIncidentId = adjudication.id.agencyIncidentId,
        reportingStaff = adjudication.incident.reportingStaff.toStaff(),
        incidentDate = adjudication.incident.incidentDate,
        incidentTime = adjudication.incident.incidentDateTime.toLocalTime(),
        reportedDate = adjudication.incident.reportedDate,
        reportedTime = adjudication.incident.reportedDateTime.toLocalTime(),
        internalLocation = adjudication.incident.agencyInternalLocation.toInternalLocation(),
        incidentType = adjudication.incident.incidentType.toCodeDescription(),
        prison = adjudication.incident.prison.toCodeDescription(),
        details = adjudication.incident.incidentDetails,
        prisonerWitnesses = adjudication.otherPrisonersInIncident { it.isWitness() },
        prisonerVictims = adjudication.otherPrisonersInIncident { it.isVictim() },
        otherPrisonersInvolved = adjudication.otherPrisonersInIncident { it.isSuspect() || it.isInvolvedForOtherReason() },
        reportingOfficers = adjudication.staffInIncident { it.isReportingOfficer() },
        staffWitnesses = adjudication.staffInIncident { it.isWitness() },
        staffVictims = adjudication.staffInIncident { it.isVictim() },
        otherStaffInvolved = adjudication.staffInIncident { it.isInvolvedForForce() || it.isInvolvedForOtherReason() },
        repairs = adjudication.incident.repairs.map { it.toRepair() },
      ),
      charges = adjudication.charges.map { it.toCharge() },
      investigations = adjudication.investigations.map { it.toInvestigation() },
      hearings = hearings.map { it.toHearing() },
    )
  }

  fun findAdjudicationIdsByFilter(
    pageRequest: Pageable,
    adjudicationFilter: AdjudicationFilter,
  ): Page<AdjudicationIdResponse> {
    return adjudicationIncidentPartyRepository.findAll(AdjudicationSpecification(adjudicationFilter), pageRequest)
      .map {
        AdjudicationIdResponse(
          adjudicationNumber = it.adjudicationNumber!!,
          offenderNo = it.offenderBooking!!.offender.nomsId,
        )
      }
  }

  fun createAdjudication(
    offenderNo: String,
    request: CreateAdjudicationRequest,
  ): AdjudicationResponse {
    val adjudicationNumber = checkAdjudicationDoesNotExist(request.adjudicationNumber)
    val prisoner = findPrisoner(offenderNo)
    val offenderBooking = findBooking(prisoner)
    val reportingStaff = findStaffByUsername(request.incident.reportingStaffUsername)
    val prison = findPrison(request.incident.prisonId)
    val internalLocation = findInternalLocation(request.incident.internalLocationId)

    return uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident(
      incidentDate = request.incident.incidentDate,
      incidentDateTime = request.incident.incidentTime.atDate(request.incident.incidentDate),
      reportedDate = request.incident.reportedDate,
      reportedDateTime = request.incident.reportedTime.atDate(request.incident.reportedDate),
      incidentType = findGovernorsReportIncidentType(),
      prison = prison,
      incidentDetails = request.incident.details,
      agencyInternalLocation = internalLocation,
      reportingStaff = reportingStaff,
    ).let { adjudicationIncidentRepository.save(it) }
      .apply {
        parties += createPrisonerAdjudicationParty(this, offenderBooking, request)
        parties += request.incident.staffWitnessesUsernames.mapIndexed { index, username ->
          createStaffWitness(
            incident = this,
            partySequence = parties.size + index + 1,
            username = username,
          )
        }
        parties += request.incident.staffVictimsUsernames.mapIndexed { index, username ->
          createStaffVictim(
            incident = this,
            partySequence = parties.size + index + 1,
            username = username,
          )
        }
        parties += request.incident.prisonerVictimsOffenderNumbers.mapIndexed { index, offenderNo ->
          createPrisonerVictim(
            incident = this,
            partySequence = parties.size + index + 1,
            offenderNo = offenderNo,
          )
        }
        repairs += request.incident.repairs.mapIndexed { index, repair ->
          createRepairForAdjudicationIncident(incident = this, index + 1, repair)
        }
      }
      .let { mapAdjudication(it.parties.findAdjudication(adjudicationNumber)) }
  }

  private fun createRepairForAdjudicationIncident(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    repairSequence: Int,
    repair: RepairToCreate,
  ) = AdjudicationIncidentRepair(
    id = AdjudicationIncidentRepairId(
      incident.id,
      repairSequence,
    ),
    type = lookupRepairType(repair.typeCode),
    comment = repair.comment,
    repairCost = repair.cost,
    incident = incident,
  )

  private fun findGovernorsReportIncidentType(): AdjudicationIncidentType {
    val governorsReportTypeId = AdjudicationIncidentType.pk(AdjudicationIncidentType.GOVERNORS_REPORT)
    return adjudicationIncidentTypeRepository.findByIdOrNull(governorsReportTypeId)!!
  }

  private fun createPrisonerAdjudicationParty(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    offenderBooking: OffenderBooking,
    request: CreateAdjudicationRequest,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = incident.parties.size + 1),
    adjudicationNumber = request.adjudicationNumber,
    offenderBooking = offenderBooking,
    incident = incident,
    incidentRole = suspectRole,
    actionDecision = lookupPlacedOnReportIncidentAction(),
  ).apply {
    this.charges += request.charges.mapIndexed { index, charge ->
      createIncidentCharge(incident, this, index + 1, charge)
    }
    request.evidence.takeIf { it.isNotEmpty() }?.let {
      this.investigations += createInvestigation(
        incident = incident,
        party = this,
        evidenceList = it,
      )
    }
  }

  private fun createStaffWitness(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    partySequence: Int,
    username: String,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = partySequence),
    staff = findStaffByUsername(username),
    incident = incident,
    incidentRole = witnessRole,
  )
  private fun createStaffVictim(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    partySequence: Int,
    username: String,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = partySequence),
    staff = findStaffByUsername(username),
    incident = incident,
    incidentRole = victimRole,
  )
  private fun createPrisonerVictim(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    partySequence: Int,
    offenderNo: String,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = partySequence),
    offenderBooking = findPrisoner(offenderNo).findLatestBooking(),
    actionDecision = lookupNoFurtherActionIncidentAction(),
    incident = incident,
    incidentRole = victimRole,
  )

  private fun lookupPlacedOnReportIncidentAction(): IncidentDecisionAction =
    incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(PLACED_ON_REPORT_ACTION_CODE))!!
  private fun lookupNoFurtherActionIncidentAction(): IncidentDecisionAction =
    incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(NO_FURTHER_ACTION_CODE))!!

  private fun createIncidentCharge(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    incidentParty: AdjudicationIncidentParty,
    chargeSequence: Int,
    charge: ChargeToCreate,
  ): AdjudicationIncidentCharge {
    return AdjudicationIncidentCharge(
      id = AdjudicationIncidentChargeId(
        agencyIncidentId = incident.id,
        chargeSequence = chargeSequence,
      ),
      incident = incident,
      partySequence = incidentParty.id.partySequence,
      incidentParty = incidentParty,
      offence = lookupOffence(charge.offenceCode),
      offenceId = charge.offenceId,
    )
  }

  private fun createInvestigation(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    party: AdjudicationIncidentParty,
    evidenceList: List<EvidenceToCreate>,
  ): AdjudicationInvestigation = AdjudicationInvestigation(
    incidentParty = party,
    investigator = incident.reportingStaff,
    comment = "Supplied by DPS",
    assignedDate = incident.reportedDate,
  ).apply {
    evidence += evidenceList.map {
      AdjudicationEvidence(
        statementDetail = it.detail,
        statementDate = incident.reportedDate,
        statementType = lookupEvidenceType(it.typeCode),
        investigation = this,
      )
    }
  }

  private fun checkAdjudicationDoesNotExist(adjudicationNumber: Long): Long {
    if (adjudicationIncidentPartyRepository.existsByAdjudicationNumber(adjudicationNumber)) {
      throw ConflictException("Adjudication $adjudicationNumber already exists")
    }
    return adjudicationNumber
  }

  private fun findPrisoner(offenderNo: String): Offender {
    return offenderRepository.findFirstByNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found")
  }

  private fun findPrison(prisonId: String): AgencyLocation {
    return agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw BadDataException("Prison $prisonId not found")
  }

  private fun findInternalLocation(internalLocationId: Long): AgencyInternalLocation {
    return agencyInternalLocationRepository.findByIdOrNull(internalLocationId)
      ?: throw BadDataException("Prison internal location $internalLocationId not found")
  }

  private fun findBooking(prisoner: Offender): OffenderBooking {
    return prisoner.bookings.firstOrNull { it.bookingSequence == 1 }
      ?: throw BadDataException("Prisoner ${prisoner.nomsId} has no bookings")
  }
  private fun Offender.findLatestBooking(): OffenderBooking {
    return this.bookings.firstOrNull { it.bookingSequence == 1 }
      ?: throw BadDataException("Prisoner ${this.nomsId} has no bookings")
  }

  private fun findStaffByUsername(reportingStaffUsername: String): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff {
    return staffUserAccountRepository.findByUsername(reportingStaffUsername)?.staff
      ?: throw BadDataException("Staff $reportingStaffUsername not found")
  }

  private fun lookupOffence(offenceCode: String): AdjudicationIncidentOffence =
    adjudicationIncidentOffenceRepository.findByCode(offenceCode)
      ?: throw BadDataException("Offence $offenceCode not found")

  private fun lookupEvidenceType(code: String): AdjudicationEvidenceType = evidenceTypeRepository.findByIdOrNull(
    AdjudicationEvidenceType.pk(code),
  ) ?: throw BadDataException("Evidence type $code not found")

  private fun lookupRepairType(code: String): AdjudicationRepairType = repairTypeRepository.findByIdOrNull(
    AdjudicationRepairType.pk(code),
  ) ?: throw BadDataException("Repair type $code not found")
}

private fun AdjudicationHearingResult.toHearingResult(): HearingResult = HearingResult(
  pleaFindingType = this.pleaFindingType?.toCodeDescription() ?: CodeDescription(
    pleaFindingCode,
    "Unknown Plea Finding Code",
  ),
  findingType = this.findingType.toCodeDescription(),
  charge = this.incidentCharge.toCharge(),
  offence = this.offence.toOffence(),
  resultAwards = this.resultAwards.map { it.toAward() },
)

private fun AdjudicationHearing.toHearing(): Hearing = Hearing(
  type = this.hearingType?.toCodeDescription(),
  comment = this.comment,
  hearingDate = this.hearingDate,
  hearingTime = this.hearingDateTime?.toLocalTime(),
  scheduleDate = this.scheduleDate,
  scheduleTime = this.scheduleDateTime?.toLocalTime(),
  internalLocation = this.agencyInternalLocation?.toInternalLocation(),
  representativeText = this.representativeText,
  hearingStaff = this.hearingStaff?.toStaff(),
  eventStatus = this.eventStatus?.toCodeDescription(),
  eventId = this.eventId,
  hearingResults = this.hearingResults.map { it.toHearingResult() },
)

private fun AdjudicationInvestigation.toInvestigation(): Investigation = Investigation(
  investigator = this.investigator.toStaff(),
  comment = this.comment,
  dateAssigned = this.assignedDate,
  evidence = this.evidence.map { it.toEvidence() },
)

private fun AdjudicationEvidence.toEvidence(): Evidence = Evidence(
  type = this.statementType.toCodeDescription(),
  date = this.statementDate,
  detail = this.statementDetail,
)

private fun AdjudicationIncidentParty.staffParties(): List<AdjudicationIncidentParty> =
  this.incident.parties.filter { it.staff != null }

private fun AdjudicationIncidentParty.prisonerParties(): List<AdjudicationIncidentParty> =
  this.incident.parties.filter { it.offenderBooking != null }

private fun AdjudicationIncidentParty.staffInIncident(filter: (AdjudicationIncidentParty) -> Boolean): List<Staff> =
  this.staffParties().filter { filter(it) }.map { it.staffParty().toStaff() }

private fun AdjudicationIncidentParty.otherPrisonersInIncident(filter: (AdjudicationIncidentParty) -> Boolean): List<Prisoner> =
  this.prisonerParties().filter { filter(it) && it != this }.map { it.prisonerParty().toPrisoner() }

private fun AdjudicationIncidentRepair.toRepair(): Repair =
  Repair(type = this.type.toCodeDescription(), comment = this.comment, cost = this.repairCost)

fun AdjudicationIncidentCharge.toCharge(): AdjudicationCharge = AdjudicationCharge(
  offence = this.offence.toOffence(),
  evidence = this.guiltyEvidence,
  reportDetail = this.reportDetails,
  offenceId = this.offenceId,
  chargeSequence = this.id.chargeSequence,
)

fun AdjudicationIncidentOffence.toOffence(): AdjudicationOffence = AdjudicationOffence(
  code = this.code,
  description = this.description,
  type = this.type?.toCodeDescription(),
)

fun AgencyInternalLocation.toInternalLocation() =
  InternalLocation(locationId = this.locationId, code = this.locationCode, description = this.description)

fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff.toStaff() =
  Staff(staffId = id, firstName = firstName, lastName = lastName)

fun AdjudicationHearingResultAward.toAward(isConsecutiveAward: Boolean = false): HearingResultAward =
  HearingResultAward(
    sanctionType = this.sanctionType?.toCodeDescription() ?: CodeDescription(
      sanctionCode,
      "Unknown Sanction Code",
    ),
    sanctionStatus = this.sanctionStatus?.toCodeDescription(),
    comment = this.comment,
    effectiveDate = this.effectiveDate,
    statusDate = this.statusDate,
    sanctionDays = this.sanctionDays,
    sanctionMonths = this.sanctionMonths,
    compensationAmount = this.compensationAmount,
    consecutiveAward = if (!isConsecutiveAward) {
      this.consecutiveHearingResultAward?.toAward(true)
    } else {
      null
    },
  )

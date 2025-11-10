package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.usernamePreferringGeneralAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingNotification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAwardId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionStatus.Companion.QUASHED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.PLACED_ON_REPORT_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SUSPECT_ROLE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VICTIM_ROLE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WITNESS_ROLE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isInvolvedForForce
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isInvolvedForOtherReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isReportingOfficer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isSuspect
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isVictim
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isWitness
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.prisonerOnReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.prisonerParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingResultAwardRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingResultRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentOffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentPartyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.staffParty
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

internal const val DPS_REFERRAL_PLACEHOLDER_HEARING = "DPS_REFERRAL_PLACEHOLDER"

@Service
@Transactional
class AdjudicationService(
  private val adjudicationIncidentPartyRepository: AdjudicationIncidentPartyRepository,
  private val adjudicationIncidentChargeRepository: AdjudicationIncidentChargeRepository,
  private val adjudicationIncidentRepository: AdjudicationIncidentRepository,
  private val adjudicationHearingRepository: AdjudicationHearingRepository,
  private val adjudicationHearingResultRepository: AdjudicationHearingResultRepository,
  private val adjudicationHearingResultAwardRepository: AdjudicationHearingResultAwardRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val adjudicationIncidentOffenceRepository: AdjudicationIncidentOffenceRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val adjudicationIncidentTypeRepository: ReferenceCodeRepository<AdjudicationIncidentType>,
  private val incidentDecisionActionRepository: ReferenceCodeRepository<IncidentDecisionAction>,
  private val evidenceTypeRepository: ReferenceCodeRepository<AdjudicationEvidenceType>,
  private val repairTypeRepository: ReferenceCodeRepository<AdjudicationRepairType>,
  private val hearingTypeRepository: ReferenceCodeRepository<AdjudicationHearingType>,
  private val pleaFindingTypeRepository: ReferenceCodeRepository<AdjudicationPleaFindingType>,
  private val findingTypeRepository: ReferenceCodeRepository<AdjudicationFindingType>,
  private val sanctionTypeRepository: ReferenceCodeRepository<AdjudicationSanctionType>,
  private val sanctionStatusRepository: ReferenceCodeRepository<AdjudicationSanctionStatus>,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getAdjudication(adjudicationNumber: Long): AdjudicationResponse = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.let {
    val hearings = adjudicationHearingRepository.findByAdjudicationNumber(adjudicationNumber)
    return mapAdjudication(it, hearings)
  }
    ?: throw NotFoundException("Adjudication not found")

  fun getAdjudicationByCharge(adjudicationNumber: Long, chargeSequence: Int): AdjudicationChargeResponse {
    getAdjudication(adjudicationNumber).let { adjudication ->
      adjudication.charges.find { it.chargeSequence == chargeSequence }?.let {
        return AdjudicationChargeResponse(
          adjudicationSequence = adjudication.adjudicationSequence,
          offenderNo = adjudication.offenderNo,
          bookingId = adjudication.bookingId,
          gender = adjudication.gender,
          currentPrison = adjudication.currentPrison,
          adjudicationNumber = adjudicationNumber,
          partyAddedDate = adjudication.partyAddedDate,
          comment = adjudication.comment,
          incident = adjudication.incident,
          charge = it,
          investigations = adjudication.investigations,
          // only use results for this charge
          hearings = adjudication.hearings.map { hearing -> hearing.copy(hearingResults = hearing.hearingResults.filter { results -> results.charge.chargeSequence == chargeSequence }) },
          hasMultipleCharges = adjudication.charges.size > 1,
        )
      }
        ?: throw NotFoundException("Adjudication charge not found. Adjudication number: $adjudicationNumber, charge sequence: $chargeSequence")
    }
  }

  private fun mapAdjudication(
    adjudication: AdjudicationIncidentParty,
    hearings: List<AdjudicationHearing> = emptyList(),
  ): AdjudicationResponse = AdjudicationResponse(
    // must be non-null in this context
    adjudicationNumber = adjudication.adjudicationNumber!!,
    adjudicationSequence = adjudication.id.partySequence,
    offenderNo = adjudication.prisonerOnReport().offender.nomsId,
    bookingId = adjudication.prisonerOnReport().bookingId,
    gender = adjudication.prisonerOnReport().offender.gender.toCodeDescription(),
    currentPrison = adjudication.prisonerOnReport().takeIf { it.active }?.location?.toCodeDescription(),
    partyAddedDate = adjudication.partyAddedDate,
    comment = adjudication.comment,
    incident = AdjudicationIncident(
      adjudicationIncidentId = adjudication.id.agencyIncidentId,
      reportingStaff = adjudication.incident.reportingStaff.toStaff(
        adjudication.incident.createUsername,
        adjudication.incident.reportedDate,
      ),
      incidentDate = adjudication.incident.incidentDate,
      incidentTime = adjudication.incident.incidentDateTime.toLocalTime(),
      reportedDate = adjudication.incident.reportedDate,
      reportedTime = adjudication.incident.reportedDateTime.toLocalTime(),
      createdByUsername = adjudication.incident.createUsername,
      createdDateTime = adjudication.incident.createDatetime,
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

  fun findAdjudicationChargeIdsByFilter(
    pageRequest: Pageable,
    adjudicationFilter: AdjudicationFilter,
  ): Page<AdjudicationChargeIdResponse> {
    val prisonIds = adjudicationFilter.prisonIds?.takeIf { it.isNotEmpty() }
    return findAllAdjudicationChargeIds(
      fromDate = adjudicationFilter.fromDate?.atStartOfDay(),
      toDate = adjudicationFilter.toDate?.plusDays(1)?.atStartOfDay(),
      prisonIds = prisonIds,
      hasPrisonFilter = prisonIds?.let { true } ?: false,
      pageRequest,
    ).map {
      AdjudicationChargeIdResponse(
        adjudicationNumber = it.getAdjudicationNumber(),
        chargeSequence = it.getChargeSequence(),
        offenderNo = it.getNomsId(),
      )
    }
  }

  fun findAllAdjudicationChargeIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    prisonIds: List<String>?,
    hasPrisonFilter: Boolean,
    pageable: Pageable,
  ): Page<AdjudicationChargeId> = if (fromDate == null && toDate == null && !hasPrisonFilter) {
    adjudicationIncidentChargeRepository.findAllAdjudicationChargeIds(pageable)
  } else {
    // optimisation: only do the complex SQL if we have a filter
    // typically we won't when run in production
    if (fromDate == null && toDate == null) {
      adjudicationIncidentChargeRepository.findAllAdjudicationChargeIds(
        prisonIds,
        pageable,
      )
    } else {
      adjudicationIncidentChargeRepository.findAllAdjudicationChargeIds(
        fromDate,
        toDate,
        prisonIds,
        hasPrisonFilter,
        pageable,
      )
    }
  }

  fun createAdjudication(
    offenderNo: String,
    request: CreateAdjudicationRequest,
  ): AdjudicationResponse {
    val adjudicationNumber = adjudicationIncidentPartyRepository.getNextAdjudicationNumber()
    val offenderBooking = findLatestBooking(offenderNo)
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
      incidentDetails = request.incident.details.truncateToUtf8Length(4000),
      agencyInternalLocation = internalLocation,
      reportingStaff = reportingStaff,
      createUsername = request.incident.reportingStaffUsername,
    ).let { adjudicationIncidentRepository.save(it) }
      .apply {
        parties += createPrisonerAdjudicationParty(
          adjudicationNumber = adjudicationNumber,
          incident = this,
          offenderBooking = offenderBooking,
          request = request,
        )
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
      .let { getAdjudication(adjudicationNumber) }
  }

  fun deleteIncident(adjudicationNumber: Long) {
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.let { party ->
      adjudicationIncidentRepository.deleteById(party.incident.id)
      telemetryClient.trackEvent(
        "incident-delete-success",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "incidentId" to party.incident.id.toString(),
        ),
        null,
      )
    } ?: let {
      telemetryClient.trackEvent(
        "incident-delete-failed",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
        ),
        null,
      )
    }
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
    adjudicationNumber: Long,
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    offenderBooking: OffenderBooking,
    request: CreateAdjudicationRequest,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = incident.parties.size + 1),
    adjudicationNumber = adjudicationNumber,
    offenderBooking = offenderBooking,
    incident = incident,
    incidentRole = SUSPECT_ROLE,
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
    incidentRole = WITNESS_ROLE,
  )

  private fun createStaffVictim(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    partySequence: Int,
    username: String,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = partySequence),
    staff = findStaffByUsername(username),
    incident = incident,
    incidentRole = VICTIM_ROLE,
  )

  private fun createPrisonerVictim(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    partySequence: Int,
    offenderNo: String,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(agencyIncidentId = incident.id, partySequence = partySequence),
    offenderBooking = findLatestBooking(offenderNo),
    actionDecision = lookupNoFurtherActionIncidentAction(),
    incident = incident,
    incidentRole = VICTIM_ROLE,
  )

  private fun lookupPlacedOnReportIncidentAction(): IncidentDecisionAction = incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(PLACED_ON_REPORT_ACTION_CODE))!!

  private fun lookupNoFurtherActionIncidentAction(): IncidentDecisionAction = incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(NO_FURTHER_ACTION_CODE))!!

  private fun createIncidentCharge(
    incident: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident,
    incidentParty: AdjudicationIncidentParty,
    chargeSequence: Int,
    charge: ChargeToCreate,
  ): AdjudicationIncidentCharge = AdjudicationIncidentCharge(
    id = AdjudicationIncidentChargeId(
      agencyIncidentId = incident.id,
      chargeSequence = chargeSequence,
    ),
    incident = incident,
    partySequence = incidentParty.id.partySequence,
    incidentParty = incidentParty,
    offence = lookupOffence(charge.offenceCode),
    offenceId = "${incidentParty.adjudicationNumber}/$chargeSequence",
  )

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

  fun createHearing(adjudicationNumber: Long, request: CreateHearingRequest): CreateHearingResponse {
    val party =
      adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.also { it.generateOffenceIds() }
        ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    val internalLocation = findInternalLocation(request.internalLocationId)
    return AdjudicationHearing(
      adjudicationNumber = adjudicationNumber,
      hearingDate = request.hearingDate,
      hearingDateTime = request.hearingTime.atDate(request.hearingDate),
      hearingType = lookupHearingType(request.hearingType),
      agencyInternalLocation = internalLocation,
      hearingParty = party,
    ).let { adjudicationHearingRepository.save(it) }.let { CreateHearingResponse(hearingId = it.id) }.also {
      telemetryClient.trackEvent(
        "hearing-created",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to it.hearingId.toString(),
        ),
        null,
      )
    }
  }

  fun getHearing(hearingId: Long): Hearing = adjudicationHearingRepository.findByIdOrNull(hearingId)?.toHearing()
    ?: throw NotFoundException("Hearing not found. Hearing Id: $hearingId")

  private fun findPrison(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId)
    ?: throw BadDataException("Prison $prisonId not found")

  private fun findInternalLocation(internalLocationId: Long): AgencyInternalLocation = agencyInternalLocationRepository.findByIdOrNull(internalLocationId)
    ?: throw BadDataException("Prison internal location $internalLocationId not found")

  private fun findLatestBooking(offenderNo: String): OffenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  private fun findStaffByUsername(reportingStaffUsername: String): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff = staffUserAccountRepository.findByUsername(reportingStaffUsername)?.staff
    ?: throw BadDataException("Staff $reportingStaffUsername not found")

  private fun lookupOffence(offenceCode: String): AdjudicationIncidentOffence = adjudicationIncidentOffenceRepository.findByCode(offenceCode)
    ?: throw BadDataException("Offence $offenceCode not found")

  private fun lookupEvidenceType(code: String): AdjudicationEvidenceType = evidenceTypeRepository.findByIdOrNull(
    AdjudicationEvidenceType.pk(code),
  ) ?: throw BadDataException("Evidence type $code not found")

  private fun lookupRepairType(code: String): AdjudicationRepairType = repairTypeRepository.findByIdOrNull(
    AdjudicationRepairType.pk(code),
  ) ?: throw BadDataException("Repair type $code not found")

  private fun lookupHearingType(code: String): AdjudicationHearingType = hearingTypeRepository.findByIdOrNull(
    AdjudicationHearingType.pk(code),
  ) ?: throw BadDataException("Hearing type $code not found")

  private fun lookupPleaFindingType(code: String): AdjudicationPleaFindingType = pleaFindingTypeRepository.findByIdOrNull(
    AdjudicationPleaFindingType.pk(code),
  ) ?: throw BadDataException("Plea finding type $code not found")

  private fun lookupFindingType(code: String): AdjudicationFindingType = findingTypeRepository.findByIdOrNull(
    AdjudicationFindingType.pk(code),
  ) ?: throw BadDataException("Finding type $code not found")

  private fun lookupSanctionType(code: String): AdjudicationSanctionType = sanctionTypeRepository.findByIdOrNull(
    AdjudicationSanctionType.pk(code),
  ) ?: throw BadDataException("sanction type $code not found")

  private fun lookupSanctionStatus(code: String): AdjudicationSanctionStatus = sanctionStatusRepository.findByIdOrNull(
    AdjudicationSanctionStatus.pk(code),
  ) ?: throw BadDataException("sanction status $code not found")

  fun updateRepairs(adjudicationNumber: Long, request: UpdateRepairsRequest): UpdateRepairsResponse {
    val adjudicationParty = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")
    val incident = adjudicationParty.incident

    // delete all previous repairs first
    adjudicationParty.incident.repairs.clear()
    adjudicationIncidentPartyRepository.saveAndFlush(adjudicationParty)

    val updatedRepairs = request.repairs.mapIndexed { index, repair ->
      AdjudicationIncidentRepair(
        id = AdjudicationIncidentRepairId(
          incident.id,
          index + 1,
        ),
        type = lookupRepairType(repair.typeCode),
        comment = repair.comment,
        incident = incident,
      )
    }
    adjudicationParty.incident.repairs.addAll(updatedRepairs)
    adjudicationIncidentPartyRepository.saveAndFlush(adjudicationParty)
    return UpdateRepairsResponse(updatedRepairs.map { it.toRepair() })
  }

  fun updateEvidence(
    adjudicationNumber: Long,
    request: UpdateEvidenceRequest,
  ): UpdateEvidenceResponse {
    val adjudicationParty =
      adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.also { it.generateOffenceIds() }
        ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    // any evidence for any investigation needs to be cleared down, ready to be replaced
    adjudicationParty.investigations.forEach { it.evidence.clear() }.also {
      adjudicationIncidentPartyRepository.saveAndFlush(adjudicationParty)
    }

    if (request.evidence.isNotEmpty()) {
      createOrGetLastInvestigation(adjudicationParty).apply {
        this.evidence.addAll(
          request.evidence.map { evidence ->
            AdjudicationEvidence(
              statementDetail = evidence.detail,
              statementDate = LocalDate.now(),
              statementType = lookupEvidenceType(evidence.typeCode),
              investigation = this,
            )
          },
        )
      }.also {
        adjudicationIncidentPartyRepository.saveAndFlush(adjudicationParty)
      }
    }
    return UpdateEvidenceResponse(evidence = adjudicationParty.toEvidenceList())
  }

  private fun createOrGetLastInvestigation(adjudicationParty: AdjudicationIncidentParty): AdjudicationInvestigation {
    val investigations = adjudicationParty.investigations
    return if (investigations.isEmpty()) {
      createInvestigation(
        incident = adjudicationParty.incident,
        party = adjudicationParty,
        evidenceList = emptyList(),
      ).also { adjudicationParty.investigations.add(it) }
    } else {
      investigations.last()
    }
  }

  fun updateHearing(adjudicationNumber: Long, hearingId: Long, request: UpdateHearingRequest): Hearing {
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")
    return adjudicationHearingRepository.findByIdOrNull(hearingId)?.let {
      val internalLocation = findInternalLocation(request.internalLocationId)
      it.hearingDate = request.hearingDate
      it.hearingDateTime = request.hearingTime.atDate(request.hearingDate)
      it.hearingType = lookupHearingType(request.hearingType)
      it.agencyInternalLocation = internalLocation
      it.toHearing()
    }.also {
      telemetryClient.trackEvent(
        "hearing-updated",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to hearingId.toString(),
        ),
        null,
      )
    } ?: throw NotFoundException("Adjudication hearing with hearing Id $hearingId not found")
  }

  fun deleteHearing(adjudicationNumber: Long, hearingId: Long) {
    // allow delete request to fail if adjudication doesn't exist as should never happen
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Hearing with id $hearingId delete failed: Adjudication party with adjudication number $adjudicationNumber not found")
    adjudicationHearingRepository.findByIdOrNull(
      hearingId,
    )?.also {
      adjudicationHearingRepository.deleteById(hearingId)
      telemetryClient.trackEvent(
        "hearing-deleted",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to hearingId.toString(),
        ),
        null,
      )
    } ?: telemetryClient.trackEvent(
      "hearing-delete-not-found",
      mapOf(
        "adjudicationNumber" to adjudicationNumber.toString(),
        "hearingId" to hearingId.toString(),
      ),
      null,
    )
  }

  fun upsertHearingResult(
    adjudicationNumber: Long,
    hearingId: Long,
    chargeSequence: Int,
    request: CreateHearingResultRequest,
  ) {
    val telemetryMap = mutableMapOf(
      "adjudicationNumber" to adjudicationNumber.toString(),
      "chargeSequence" to chargeSequence.toString(),
      "hearingId" to hearingId.toString(),
      "findingCode" to request.findingCode,
      "plea" to request.pleaFindingCode,
    )

    // DPS only allows 1 result per hearing, the created result has a result sequence of 1
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    // DPS created (or migrated) adjudications will have 1 charge
    val incidentCharge = party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number $adjudicationNumber and charge sequence $chargeSequence")

    val hearing = adjudicationHearingRepository.findByIdOrNull(hearingId)
      ?: throw NotFoundException("Hearing not found. Hearing Id: $hearingId")

    request.adjudicatorUsername?.let { hearing.hearingStaff = findStaffByUsername(request.adjudicatorUsername) }

    val existingResult = adjudicationHearingResultRepository.findFirstOrNullByIdOicHearingIdAndChargeSequence(
      chargeSequence = chargeSequence,
      hearingId = hearingId,
    )

    existingResult?.let {
      it.pleaFindingType = lookupPleaFindingType(request.pleaFindingCode)
      it.pleaFindingCode = request.pleaFindingCode
      it.findingType = lookupFindingType(request.findingCode)
      adjudicationHearingResultRepository.saveAndFlush(it)
      telemetryMap["resultSequence"] = it.id.resultSequence.toString()
      telemetryClient.trackEvent(
        "hearing-result-updated",
        telemetryMap,
        null,
      )
    } ?: let {
      val resultSeq = hearing.hearingResults.highestSequence() + 1
      AdjudicationHearingResult(
        id = AdjudicationHearingResultId(oicHearingId = hearingId, resultSeq),
        incident = party.incident,
        pleaFindingType = lookupPleaFindingType(request.pleaFindingCode),
        pleaFindingCode = request.pleaFindingCode,
        findingType = lookupFindingType(request.findingCode),
        hearing = hearing,
        chargeSequence = chargeSequence,
        incidentCharge = incidentCharge,
        offence = incidentCharge.offence,
      ).let { adjudicationHearingResultRepository.saveAndFlush(it) }
        .also {
          telemetryMap["resultSequence"] = it.id.resultSequence.toString()
          telemetryClient.trackEvent(
            "hearing-result-created",
            telemetryMap,
            null,
          )
        }
    }
  }

  fun getHearingResult(hearingId: Long, chargeSequence: Int): HearingResult = adjudicationHearingResultRepository.findFirstOrNullByIdOicHearingIdAndChargeSequence(hearingId, chargeSequence)
    ?.toHearingResult()
    ?: throw NotFoundException("Hearing Result not found. Hearing Id: $hearingId, charge sequence: $chargeSequence")

  fun deleteHearingResult(adjudicationNumber: Long, hearingId: Long, chargeSequence: Int): DeleteHearingResultResponse {
    // allow delete request to fail if adjudication doesn't exist as should never happen
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Hearing with id $hearingId delete failed: Adjudication party with adjudication number $adjudicationNumber not found")

    return adjudicationHearingResultRepository.findFirstOrNullByIdOicHearingIdAndChargeSequence(
      hearingId = hearingId,
      chargeSequence = chargeSequence,
    )?.let { result ->
      val deletedAwards = result.resultAwards
      adjudicationHearingResultRepository.delete(result)
      adjudicationHearingRepository.findByIdOrNull(hearingId)?.let { it.hearingStaff = null }
      telemetryClient.trackEvent(
        "hearing-result-deleted",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to hearingId.toString(),
          "resultSequence" to result.id.resultSequence.toString(),
        ),
        null,
      )
      DeleteHearingResultResponse(
        deletedAwards.map {
          HearingResultAwardResponse(
            it.id.offenderBookId,
            it.id.sanctionSequence,
          )
        },
      )
    } ?: let {
      telemetryClient.trackEvent(
        "hearing-result-delete-not-found",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to hearingId.toString(),
          "chargeSequence" to chargeSequence.toString(),
        ),
        null,
      )
      DeleteHearingResultResponse()
    }
  }

  fun createHearingResultAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
    requests: CreateHearingResultAwardRequest,
  ): CreateHearingResultAwardResponses {
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    val offenderBookId = party.prisonerOnReport().bookingId

    val sanctionSeq =
      adjudicationHearingResultAwardRepository.getNextSanctionSequence(offenderBookId = offenderBookId)

    return CreateHearingResultAwardResponses(
      createHearingResultAwards(
        party = party,
        chargeSequence = chargeSequence,
        sanctionSeq = sanctionSeq,
        awardsToCreate = requests.awards,
      ),
    )
  }

  private fun createHearingResultAwards(
    party: AdjudicationIncidentParty,
    chargeSequence: Int,
    sanctionSeq: Int,
    awardsToCreate: List<HearingResultAwardRequest>,
  ): List<HearingResultAwardResponse> {
    val offenderBookId = party.prisonerOnReport().bookingId

    val incidentCharge = party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number ${party.adjudicationNumber} and charge sequence $chargeSequence")

    // find the latest result on the latest hearing
    val hearingResult =
      adjudicationHearingResultRepository.findFirstOrNullByIncidentChargeOrderByIdOicHearingIdDescIdResultSequenceDesc(
        incidentCharge,
      ) ?: throw BadDataException("Hearing result for adjudication number ${party.adjudicationNumber} not found")

    return awardsToCreate.mapIndexed { index, request ->
      AdjudicationHearingResultAward(
        id = AdjudicationHearingResultAwardId(
          offenderBookId = offenderBookId,
          sanctionSequence = sanctionSeq + index,
        ),
        incidentParty = party,
        sanctionType = lookupSanctionType(request.sanctionType),
        sanctionCode = request.sanctionType,
        compensationAmount = request.compensationAmount,
        effectiveDate = request.effectiveDate,
        sanctionDays = request.sanctionDays,
        comment = request.commentText,
        sanctionStatus = lookupSanctionStatus(request.sanctionStatus),
        hearingResult = hearingResult,
        consecutiveHearingResultAward = request.consecutiveCharge
          ?.let {
            findMatchingSanctionAwardForAdjudicationCharge(
              adjudicationNumber = it.adjudicationNumber,
              chargeSequence = it.chargeSequence,
              sanctionCode = request.sanctionType,
            )
          },
      ).let { adjudicationHearingResultAwardRepository.save(it) }
        .also {
          telemetryClient.trackEvent(
            "hearing-result-award-created",
            mapOf(
              "adjudicationNumber" to party.adjudicationNumber.toString(),
              "sanctionSequence" to sanctionSeq.toString(),
              "bookingId" to offenderBookId.toString(),
              "resultSequence" to hearingResult.id.resultSequence.toString(),
              "hearingId" to hearingResult.id.oicHearingId.toString(),
            ),
            null,
          )
        }
        .let {
          HearingResultAwardResponse(
            bookingId = it.id.offenderBookId,
            sanctionSequence = it.id.sanctionSequence,
          )
        }
    }
  }

  fun updateCreateAndDeleteHearingResultAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
    requests: UpdateHearingResultAwardRequest,
  ): UpdateHearingResultAwardResponses {
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")
    val offenderBookId = party.prisonerOnReport().bookingId
    // grab next sequence before we start deleting awards
    val sanctionSeq = adjudicationHearingResultAwardRepository.getNextSanctionSequence(offenderBookId = offenderBookId)

    val deletedAwards = deleteAwards(
      adjudicationNumber = adjudicationNumber,
      chargeSequence = chargeSequence,
      sanctionsToKeep = requests.awardsToUpdate.map { it.sanctionSequence },
    )
    val createdAwards = createHearingResultAwards(
      party = party,
      sanctionSeq = sanctionSeq,
      chargeSequence = chargeSequence,
      awardsToCreate = requests.awardsToCreate,
    )
    updateHearingResultAwards(offenderBookId, adjudicationNumber, requests.awardsToUpdate)
    return UpdateHearingResultAwardResponses(awardsCreated = createdAwards, awardsDeleted = deletedAwards)
  }

  fun deleteHearingResultAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
  ): DeleteHearingResultAwardResponses {
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")
    party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number $adjudicationNumber and charge sequence $chargeSequence")

    return deleteAwards(
      adjudicationNumber = adjudicationNumber,
      chargeSequence = chargeSequence,
    ).let { DeleteHearingResultAwardResponses(it) }
  }

  private fun deleteAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
    sanctionsToKeep: List<Int> = emptyList(),
  ): List<HearingResultAwardResponse> {
    val allAwards =
      adjudicationHearingResultAwardRepository.findByIncidentPartyAdjudicationNumberAndHearingResultChargeSequenceOrderByIdSanctionSequence(
        adjudicationNumber,
        chargeSequence = chargeSequence,
      )

    val sanctionsToDelete = allAwards.filterNot { sanctionsToKeep.contains(it.id.sanctionSequence) }
    return sanctionsToDelete.map {
      adjudicationHearingResultAwardRepository.delete(it)
      it.hearingResult?.resultAwards?.remove(it)
      telemetryClient.trackEvent(
        "hearing-result-award-deleted",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "sanctionSequence" to it.id.sanctionSequence.toString(),
          "bookingId" to it.id.offenderBookId.toString(),
        ),
        null,
      )
      HearingResultAwardResponse(it.id.offenderBookId, it.id.sanctionSequence)
    }
  }

  private fun squashHearingResultAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
  ) {
    val allAwards =
      adjudicationHearingResultAwardRepository.findByIncidentPartyAdjudicationNumberAndHearingResultChargeSequenceOrderByIdSanctionSequence(
        adjudicationNumber,
        chargeSequence = chargeSequence,
      )

    allAwards.forEach {
      it.sanctionStatus = lookupSanctionStatus(QUASHED)
      telemetryClient.trackEvent(
        "hearing-result-award-quashed",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "sanctionSequence" to it.id.sanctionSequence.toString(),
          "bookingId" to it.id.offenderBookId.toString(),
        ),
        null,
      )
    }
  }

  private fun updateHearingResultAwards(
    offenderBookId: Long,
    adjudicationNumber: Long,
    awardRequestsToUpdate: List<ExistingHearingResultAwardRequest>,
  ) {
    awardRequestsToUpdate.forEach {
      val award = adjudicationHearingResultAwardRepository.findByIdOrNull(
        AdjudicationHearingResultAwardId(
          offenderBookId,
          it.sanctionSequence,
        ),
      )
        ?: throw NotFoundException("Hearing result award not found. booking Id: $offenderBookId, sanction sequence: ${it.sanctionSequence}")

      with(award) {
        sanctionType = lookupSanctionType(it.award.sanctionType)
        sanctionCode = it.award.sanctionType
        compensationAmount = it.award.compensationAmount
        effectiveDate = it.award.effectiveDate
        sanctionDays = it.award.sanctionDays
        comment = it.award.commentText
        sanctionStatus = lookupSanctionStatus(it.award.sanctionStatus)
        consecutiveHearingResultAward = it.award.consecutiveCharge
          ?.let { charge ->
            findMatchingSanctionAwardForAdjudicationCharge(
              adjudicationNumber = charge.adjudicationNumber,
              chargeSequence = charge.chargeSequence,
              sanctionCode = it.award.sanctionType,
            )
          }

        telemetryClient.trackEvent(
          "hearing-result-award-updated",
          mapOf(
            "adjudicationNumber" to adjudicationNumber.toString(),
            "sanctionSequence" to it.sanctionSequence.toString(),
            "bookingId" to offenderBookId.toString(),
          ),
          null,
        )
      }
    }
  }

  private fun findMatchingSanctionAwardForAdjudicationCharge(
    adjudicationNumber: Long,
    chargeSequence: Int,
    sanctionCode: String,
  ): AdjudicationHearingResultAward = adjudicationHearingResultAwardRepository.findFirstOrNullByIncidentPartyAdjudicationNumberAndSanctionCodeAndHearingResultChargeSequence(
    adjudicationNumber = adjudicationNumber,
    sanctionCode = sanctionCode,
    chargeSequence = chargeSequence,
  )
    ?: throw BadDataException("Matching consecutive adjudication award not found. Adjudication number: $adjudicationNumber, charge sequence: $chargeSequence, sanction code: $sanctionCode")

  fun getHearingResultAward(bookingId: Long, sanctionSequence: Int): HearingResultAward = adjudicationHearingResultAwardRepository.findByIdOrNull(
    AdjudicationHearingResultAwardId(
      bookingId,
      sanctionSequence,
    ),
  )
    ?.toAward()
    ?: throw NotFoundException("Hearing Result Award not found. booking Id: $bookingId, sanction sequence: $sanctionSequence")

  fun upsertResultWithDummyHearing(
    adjudicationNumber: Long,
    chargeSequence: Int,
    request: CreateHearingResultRequest,
  ) {
    val telemetryMap = mutableMapOf(
      "adjudicationNumber" to adjudicationNumber.toString(),
      "chargeSequence" to chargeSequence.toString(),
      "findingCode" to request.findingCode,
      "plea" to request.pleaFindingCode,
    )

    val party =
      adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.also { it.generateOffenceIds() }
        ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    // DPS created (or migrated) adjudications will have 1 charge
    val incidentCharge = party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number $adjudicationNumber and charge sequence $chargeSequence")

    val adjudicationRoom =
      agencyInternalLocationRepository.findAgencyInternalLocationsByAgencyIdAndLocationTypeAndActive(
        agencyId = incidentCharge.incident.prison.id,
        locationType = "ADJU",
        active = true,
      ).firstOrNull()
        ?: agencyInternalLocationRepository.findAgencyInternalLocationsByAgencyIdAndLocationTypeAndActive(
          agencyId = incidentCharge.incident.prison.id,
          locationType = "ADJU",
          active = false,
        ).firstOrNull()
          .also { log.warn("An active adjudication room (location type ADJU) not found at prison ${incidentCharge.incident.prison.id} so checking for any inactive rooms, assuming this prison is now closed") }
        ?: throw NotFoundException("Adjudication room (location type ADJU) not found at prison ${incidentCharge.incident.prison.id}")

    val dummyHearingIdentifier = "$DPS_REFERRAL_PLACEHOLDER_HEARING-$chargeSequence"
    val existingDummyHearing = adjudicationHearingRepository.findByAdjudicationNumberAndComment(
      adjudicationNumber = adjudicationNumber,
      comment = dummyHearingIdentifier,
    )

    val hearing = existingDummyHearing ?: let {
      AdjudicationHearing(
        adjudicationNumber = adjudicationNumber,
        hearingDate = LocalDate.now(),
        hearingDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT),
        hearingType = lookupHearingType(AdjudicationHearingType.GOVERNORS_HEARING),
        agencyInternalLocation = adjudicationRoom,
        hearingParty = party,
        comment = dummyHearingIdentifier,
      ).let { adjudicationHearingRepository.save(it) }
    }

    telemetryMap["hearingId"] = hearing.id.toString()

    hearing.hearingResults.firstOrNull()?.let {
      it.pleaFindingType = lookupPleaFindingType(request.pleaFindingCode)
      it.pleaFindingCode = request.pleaFindingCode
      it.findingType = lookupFindingType(request.findingCode)
      adjudicationHearingResultRepository.saveAndFlush(it)
      telemetryMap["resultSequence"] = it.id.resultSequence.toString()
      telemetryClient.trackEvent(
        "hearing-result-updated",
        telemetryMap,
        null,
      )
    } ?: let {
      AdjudicationHearingResult(
        id = AdjudicationHearingResultId(oicHearingId = hearing.id, 1),
        incident = party.incident,
        pleaFindingType = lookupPleaFindingType(request.pleaFindingCode),
        pleaFindingCode = request.pleaFindingCode,
        findingType = lookupFindingType(request.findingCode),
        hearing = hearing,
        chargeSequence = chargeSequence,
        incidentCharge = incidentCharge,
        offence = incidentCharge.offence,
      ).let { adjudicationHearingResultRepository.saveAndFlush(it) }
        .also {
          telemetryMap["resultSequence"] = it.id.resultSequence.toString()
          telemetryClient.trackEvent(
            "hearing-result-created",
            telemetryMap,
            null,
          )
        }
    }
  }

  fun deleteResultWithDummyHearing(adjudicationNumber: Long, chargeSequence: Int) {
    // allow delete request to fail if adjudication doesn't exist as should never happen
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    // a referral result must have an associated dummy hearing. Delete the hearing, which will cascade and delete the result.
    adjudicationHearingRepository.findByAdjudicationNumberAndComment(
      adjudicationNumber,
      "$DPS_REFERRAL_PLACEHOLDER_HEARING-$chargeSequence",
    )?.let {
      adjudicationHearingRepository.delete(it)
      telemetryClient.trackEvent(
        "hearing-result-deleted",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "hearingId" to it.id.toString(),
          "resultSequence" to it.hearingResults.firstOrNull()?.id?.resultSequence.toString(),
        ),
        null,
      )
    } ?: let {
      telemetryClient.trackEvent(
        "hearing-result-delete-not-found",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
          "chargeSequence" to chargeSequence.toString(),
        ),
        null,
      )
    }
  }

  fun quashHearingResultAndAwards(adjudicationNumber: Long, chargeSequence: Int) {
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    val incidentCharge = party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number $adjudicationNumber and charge sequence $chargeSequence")

    // find the latest result on the latest hearing
    val hearingResult =
      adjudicationHearingResultRepository.findFirstOrNullByIncidentChargeOrderByIdOicHearingIdDescIdResultSequenceDesc(
        incidentCharge,
      ) ?: throw BadDataException("Hearing result for adjudication number ${party.adjudicationNumber} not found")

    hearingResult.findingType = lookupFindingType(AdjudicationFindingType.QUASHED)

    squashHearingResultAwards(adjudicationNumber, chargeSequence)

    telemetryClient.trackEvent(
      "hearing-result-quashed",
      mapOf(
        "adjudicationNumber" to adjudicationNumber.toString(),
        "chargeSequence" to chargeSequence.toString(),
        "hearingId" to hearingResult.id.oicHearingId.toString(),
        "offenderNo" to party.prisonerParty().nomsId,
      ),
      null,
    )
  }

  fun unquashHearingResultAndAwards(
    adjudicationNumber: Long,
    chargeSequence: Int,
    request: UnquashHearingResultAwardRequest,
  ): UpdateHearingResultAwardResponses {
    val party = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)
      ?: throw NotFoundException("Adjudication party with adjudication number $adjudicationNumber not found")

    val incidentCharge = party.charges.firstOrNull { it.id.chargeSequence == chargeSequence }
      ?: throw NotFoundException("Charge not found for adjudication number $adjudicationNumber and charge sequence $chargeSequence")

    val hearingResult =
      adjudicationHearingResultRepository.findFirstOrNullByIncidentChargeOrderByIdOicHearingIdDescIdResultSequenceDesc(
        incidentCharge,
      ) ?: throw BadDataException("Hearing result for adjudication number ${party.adjudicationNumber} not found")

    hearingResult.findingType = lookupFindingType(request.findingCode)
    return updateCreateAndDeleteHearingResultAwards(
      adjudicationNumber = adjudicationNumber,
      chargeSequence = chargeSequence,
      requests = request.awards,
    )
  }

  private fun AdjudicationHearingResultAward.asDays() = this.sanctionDays + this.sanctionMonths.asDays(this.effectiveDate)

  private operator fun Int?.plus(second: Int?): Int? = when {
    this == null && second == null -> null
    this == null -> second
    second == null -> this
    else -> this + second
  }

  private fun Int?.asDays(effectiveDate: LocalDate): Int? = this?.let { ChronoUnit.DAYS.between(effectiveDate, effectiveDate.plusMonths(this.toLong())).toInt() }

  fun getADAHearingResultAwardSummary(bookingId: Long): AdjudicationADAAwardSummaryResponse {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Prisoner with bookingId $bookingId not found")

    val awards =
      adjudicationHearingResultAwardRepository.findByIdOffenderBookIdAndSanctionCodeOrderByIdSanctionSequenceAsc(
        offenderBookId = bookingId,
        sanctionCode = "ADA",
      )
    return AdjudicationADAAwardSummaryResponse(
      bookingId,
      offenderNo = booking.offender.nomsId,
      adaSummaries = awards.map {
        ADASummary(
          adjudicationNumber = it.incidentParty.adjudicationNumber!!,
          days = it.asDays() ?: 0,
          effectiveDate = it.effectiveDate,
          sanctionSequence = it.id.sanctionSequence,
          sanctionStatus = it.sanctionStatus?.toCodeDescription() ?: CodeDescription("UNKNOWN", "UNKNOWN"),
        )
      },
    )
  }
}

private fun Iterable<AdjudicationHearingResult>.highestSequence(): Int = this.maxOfOrNull { it.id.resultSequence } ?: 0

private fun AdjudicationIncidentParty.generateOffenceIds() {
  this.charges
    .filter { it.offenceId == null }
    .forEach {
      it.offenceId = "${this.adjudicationNumber}/${it.id.chargeSequence}"
    }
}

private fun AdjudicationHearingResult.toHearingResult(): HearingResult = HearingResult(
  pleaFindingType = this.pleaFindingType?.toCodeDescription() ?: CodeDescription(
    pleaFindingCode,
    "Unknown Plea Finding Code",
  ),
  findingType = this.findingType.toCodeDescription(),
  // this is only called when this is non-null
  charge = this.incidentCharge?.toCharge()!!,
  offence = this.offence.toOffence(),
  resultAwards = this.resultAwards.filter { it.matchesAdjudicationParty() }.map { it.toAward() },
  createdDateTime = this.whenCreated,
  createdByUsername = this.createUsername,
)

// edge case for NOMIS merge where two sets of sanctions for different bookings attached
// to same hearing result. There is only one case of this in production so this simple fix is preferred over
// a complicated mapping change
private fun AdjudicationHearingResultAward.matchesAdjudicationParty(): Boolean = this.id.offenderBookId == this.incidentParty.offenderBooking?.bookingId

private fun AdjudicationHearing.toHearing(): Hearing = Hearing(
  hearingId = this.id,
  type = this.hearingType?.toCodeDescription(),
  comment = this.comment,
  hearingDate = this.hearingDate,
  hearingTime = this.hearingDateTime?.toLocalTime(),
  scheduleDate = this.scheduleDate,
  scheduleTime = this.scheduleDateTime?.toLocalTime(),
  internalLocation = this.agencyInternalLocation?.toInternalLocation(),
  representativeText = this.representativeText,
  hearingStaff = this.hearingStaff?.toStaff(this.createUsername, this.whenCreated.toLocalDate()),
  eventStatus = this.eventStatus?.toCodeDescription(),
  eventId = this.eventId,
  // remove bad data, results with no charges
  hearingResults = this.hearingResults.filter { it.incidentCharge != null }.map { it.toHearingResult() },
  createdDateTime = this.whenCreated,
  createdByUsername = this.createUsername,
  notifications = this.hearingNotifications.map { it.toNotification() },
)

private fun AdjudicationHearingNotification.toNotification(): HearingNotification = HearingNotification(
  deliveryDate = this.deliveryDate,
  deliveryTime = this.deliveryDateTime.toLocalTime(),
  comment = this.comment,
  notifiedStaff = this.deliveryStaff.toStaff(),
)

private fun AdjudicationInvestigation.toInvestigation(): Investigation = Investigation(
  investigator = this.investigator.toStaff(createUsername, this.assignedDate),
  comment = this.comment,
  dateAssigned = this.assignedDate,
  evidence = this.evidence.map { it.toEvidence() },
)

private fun AdjudicationEvidence.toEvidence(): Evidence = Evidence(
  type = this.statementType.toCodeDescription(),
  date = this.statementDate,
  detail = this.statementDetail,
  createdByUsername = this.createUsername,
)

private fun AdjudicationIncidentParty.toEvidenceList(): List<Evidence> = this.investigations.flatMap { investigation -> investigation.evidence.map { it.toEvidence() } }

private fun AdjudicationIncidentParty.staffParties(): List<AdjudicationIncidentParty> = this.incident.parties.filter { it.staff != null }

private fun AdjudicationIncidentParty.prisonerParties(): List<AdjudicationIncidentParty> = this.incident.parties.filter { it.offenderBooking != null }

private fun AdjudicationIncidentParty.staffInIncident(filter: (AdjudicationIncidentParty) -> Boolean): List<Staff> = this.staffParties().filter { filter(it) }
  .map { it.staffParty().toStaff(it.createUsername, it.partyAddedDate, it.comment) }

private fun AdjudicationIncidentParty.otherPrisonersInIncident(filter: (AdjudicationIncidentParty) -> Boolean): List<Prisoner> = this.prisonerParties().filter { filter(it) && it != this }
  .map { it.prisonerParty().toPrisoner(it.createUsername, it.partyAddedDate, it.comment) }

private fun AdjudicationIncidentRepair.toRepair(): Repair = Repair(
  type = this.type.toCodeDescription(),
  comment = this.comment,
  cost = this.repairCost,
  createdByUsername = this.createUsername,
)

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

fun AgencyInternalLocation.toInternalLocation() = InternalLocation(locationId = this.locationId, code = this.locationCode, description = this.description)

fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff.toStaff(
  createUsername: String,
  dateAddedToIncident: LocalDate,
  comment: String? = null,
) = Staff(
  staffId = id,
  firstName = firstName,
  lastName = lastName,
  username = accounts.usernamePreferringGeneralAccount(),
  createdByUsername = createUsername,
  dateAddedToIncident = dateAddedToIncident,
  comment = comment,
)

fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff.toStaff() = Staff(
  staffId = id,
  firstName = firstName,
  lastName = lastName,
  username = accounts.usernamePreferringGeneralAccount(),
)

fun AdjudicationHearingResultAward.toAward(isConsecutiveAward: Boolean = false): HearingResultAward = HearingResultAward(
  sequence = this.id.sanctionSequence,
  // we must have result for there to be an award
  chargeSequence = this.hearingResult?.chargeSequence!!,
  adjudicationNumber = this.hearingResult.hearing.adjudicationNumber,
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
  createdByUsername = this.createUsername,
  createdDateTime = this.whenCreated,
  compensationAmount = this.compensationAmount,
  consecutiveAward = if (!isConsecutiveAward) {
    this.consecutiveHearingResultAward?.toAward(true)
  } else {
    null
  },
)

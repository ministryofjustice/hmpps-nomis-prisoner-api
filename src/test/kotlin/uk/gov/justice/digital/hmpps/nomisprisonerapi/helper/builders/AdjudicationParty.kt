package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class AdjudicationPartyBuilderFactory(
  private val repository: AdjudicationPartyBuilderRepository,
  private val adjudicationChargeBuilderFactory: AdjudicationChargeBuilderFactory,
  private val adjudicationInvestigationBuilderFactory: AdjudicationInvestigationBuilderFactory,
  private val adjudicationHearingBuilderFactory: AdjudicationHearingBuilderFactory,
) {
  fun builder(): AdjudicationPartyBuilder {
    return AdjudicationPartyBuilder(
      repository,
      adjudicationChargeBuilderFactory,
      adjudicationInvestigationBuilderFactory,
      adjudicationHearingBuilderFactory,
    )
  }
}

@Component
class AdjudicationPartyBuilderRepository(
  val incidentDecisionActionRepository: ReferenceCodeRepository<IncidentDecisionAction>,
) {
  fun lookupActionDecision(code: String = IncidentDecisionAction.PLACED_ON_REPORT_ACTION_CODE): IncidentDecisionAction =
    incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(code))!!
}

class AdjudicationPartyBuilder(
  private val repository: AdjudicationPartyBuilderRepository,
  private val adjudicationChargeBuilderFactory: AdjudicationChargeBuilderFactory,
  private val adjudicationInvestigationBuilderFactory: AdjudicationInvestigationBuilderFactory,
  private val adjudicationHearingBuilderFactory: AdjudicationHearingBuilderFactory,
) : AdjudicationPartyDsl {
  private lateinit var adjudicationParty: AdjudicationIncidentParty

  fun build(
    adjudicationNumber: Long? = null,
    comment: String,
    staff: Staff?,
    incidentRole: String,
    actionDecision: String,
    partyAddedDate: LocalDate,
    incident: AdjudicationIncident,
    offenderBooking: OffenderBooking?,
    whenCreated: LocalDateTime,
    index: Int,
  ): AdjudicationIncidentParty = AdjudicationIncidentParty(
    id = AdjudicationIncidentPartyId(incident.id, index),
    offenderBooking = offenderBooking,
    staff = staff,
    adjudicationNumber = adjudicationNumber,
    incidentRole = incidentRole,
    incident = incident,
    actionDecision = repository.lookupActionDecision(actionDecision),
    partyAddedDate = partyAddedDate,
    comment = comment,
    whenCreated = whenCreated,
  )
    .also { adjudicationParty = it }

  override fun investigation(
    investigator: Staff,
    comment: String?,
    assignedDate: LocalDate,
    dsl: AdjudicationInvestigationDsl.() -> Unit,
  ) =
    adjudicationInvestigationBuilderFactory.builder().let { builder ->
      builder.build(
        investigator = investigator,
        comment = comment,
        assignedDate = assignedDate,
        incidentParty = adjudicationParty,
      )
        .also { adjudicationParty.investigations += it }
        .also { builder.apply(dsl) }
    }

  override fun charge(
    offenceCode: String,
    guiltyEvidence: String?,
    reportDetail: String?,
    dsl: AdjudicationChargeDsl.() -> Unit,
  ) =
    adjudicationChargeBuilderFactory.builder().let { builder ->
      builder.build(
        offenceCode = offenceCode,
        guiltyEvidence = guiltyEvidence,
        reportDetail = reportDetail,
        incidentParty = adjudicationParty,
        chargeSequence = adjudicationParty.incident.parties.sumOf { it.charges.size } + 1,
      )
        .also { adjudicationParty.charges += it }
        .also { builder.apply(dsl) }
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
  ) =
    adjudicationHearingBuilderFactory.builder().let { builder ->
      builder.build(
        agencyInternalLocationId = internalLocationId,
        scheduledDate = scheduleDate,
        scheduledDateTime = scheduleTime,
        hearingDate = hearingDate,
        hearingDateTime = hearingTime,
        hearingStaff = hearingStaff,
        hearingTypeCode = hearingTypeCode,
        eventStatusCode = eventStatusCode,
        comment = comment,
        representativeText = representativeText,
        incidentParty = adjudicationParty,
      )
        .also { builder.apply(dsl) }
    }
}

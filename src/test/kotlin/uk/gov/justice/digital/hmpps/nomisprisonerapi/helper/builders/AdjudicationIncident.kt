package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AdjudicationIncidentDslMarker

@NomisDataDslMarker
interface AdjudicationIncidentDsl {
  @AdjudicationRepairDslMarker
  fun repair(
    repairType: String = "CLEA",
    comment: String? = null,
    repairCost: BigDecimal? = null,
    dsl: AdjudicationRepairDsl.() -> Unit = {},
  ): AdjudicationIncidentRepair

  @AdjudicationPartyDslMarker
  fun party(
    comment: String = "They witnessed everything",
    role: PartyRole = WITNESS,
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    offenderBooking: OffenderBooking? = null,
    staff: Staff? = null,
    adjudicationNumber: Long? = null,
    actionDecision: String = IncidentDecisionAction.NO_FURTHER_ACTION_CODE,
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  ): AdjudicationIncidentParty
}

@Component
class AdjudicationIncidentBuilderFactory(
  private val repository: AdjudicationIncidentBuilderRepository,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val adjudicationRepairBuilderFactory: AdjudicationRepairBuilderFactory,
) {
  fun builder(): AdjudicationIncidentBuilder {
    return AdjudicationIncidentBuilder(repository, adjudicationPartyBuilderFactory, adjudicationRepairBuilderFactory)
  }
}

@Component
class AdjudicationIncidentBuilderRepository(
  private val adjudicationIncidentRepository: AdjudicationIncidentRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val adjudicationIncidentTypeRepository: ReferenceCodeRepository<AdjudicationIncidentType>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(adjudicationIncident: AdjudicationIncident): AdjudicationIncident =
    adjudicationIncidentRepository.save(adjudicationIncident)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupIncidentType(): AdjudicationIncidentType =
    adjudicationIncidentTypeRepository.findByIdOrNull(AdjudicationIncidentType.pk(AdjudicationIncidentType.GOVERNORS_REPORT))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

class AdjudicationIncidentBuilder(
  private val repository: AdjudicationIncidentBuilderRepository,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val adjudicationRepairBuilderFactory: AdjudicationRepairBuilderFactory,
) : AdjudicationIncidentDsl {
  private lateinit var adjudicationIncident: AdjudicationIncident
  private lateinit var whenCreated: LocalDateTime

  fun build(
    whenCreated: LocalDateTime,
    incidentDetails: String,
    reportedDateTime: LocalDateTime,
    reportedDate: LocalDate,
    incidentDateTime: LocalDateTime,
    incidentDate: LocalDate,
    prisonId: String,
    agencyInternalLocationId: Long,
    reportingStaff: Staff,
  ): AdjudicationIncident = AdjudicationIncident(
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
    .let { repository.save(it) }
    .also { adjudicationIncident = it }
    .also { this.whenCreated = whenCreated }

  override fun repair(
    repairType: String,
    comment: String?,
    repairCost: BigDecimal?,
    dsl: AdjudicationRepairDsl.() -> Unit,
  ) =
    adjudicationRepairBuilderFactory.builder().let { builder ->
      builder.build(
        repairType,
        comment,
        repairCost,
        incident = adjudicationIncident,
        repairSequence = adjudicationIncident.repairs.size + 1,
      )
        .also { adjudicationIncident.repairs += it }
        .also { builder.apply(dsl) }
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
  ) =
    adjudicationPartyBuilderFactory.builder().build(
      adjudicationNumber = adjudicationNumber,
      comment = comment,
      staff = staff,
      incidentRole = role.code,
      actionDecision = actionDecision,
      partyAddedDate = partyAddedDate,
      incident = adjudicationIncident,
      offenderBooking = offenderBooking,
      whenCreated = whenCreated,
      index = adjudicationIncident.parties.size + 1,
    )
      .also { adjudicationIncident.parties += it }
}

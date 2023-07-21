package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentOffenceRepository

@DslMarker
annotation class AdjudicationChargeDslMarker

@NomisDataDslMarker
interface AdjudicationChargeDsl

@Component
class AdjudicationChargeBuilderFactory(
  private val repository: AdjudicationChargeBuilderRepository,
) {
  fun builder(): AdjudicationChargeBuilder {
    return AdjudicationChargeBuilder(repository)
  }
}

@Component
class AdjudicationChargeBuilderRepository(
  val adjudicationIncidentOffenceRepository: AdjudicationIncidentOffenceRepository,
) {
  fun lookupAdjudicationOffence(code: String): AdjudicationIncidentOffence =
    adjudicationIncidentOffenceRepository.findByCode(code)!!
}

class AdjudicationChargeBuilder(
  private val repository: AdjudicationChargeBuilderRepository,
) : AdjudicationChargeDsl {
  private lateinit var adjudicationCharge: AdjudicationIncidentCharge

  fun build(
    offenceCode: String,
    guiltyEvidence: String?,
    reportDetail: String?,
    incidentParty: AdjudicationIncidentParty,
    chargeSequence: Int,
  ): AdjudicationIncidentCharge = AdjudicationIncidentCharge(
    id = AdjudicationIncidentChargeId(incidentParty.id.agencyIncidentId, chargeSequence),
    incident = incidentParty.incident,
    partySequence = incidentParty.id.partySequence,
    incidentParty = incidentParty,
    offence = repository.lookupAdjudicationOffence(offenceCode),
    guiltyEvidence = guiltyEvidence,
    reportDetails = reportDetail,
    offenceId = "${incidentParty.adjudicationNumber}/$chargeSequence",
  )
    .also { adjudicationCharge = it }
}

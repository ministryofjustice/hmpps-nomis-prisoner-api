package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepairId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationRepairType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal

@DslMarker
annotation class AdjudicationRepairDslMarker

@NomisDataDslMarker
interface AdjudicationRepairDsl

@Component
class AdjudicationRepairBuilderFactory(
  private val repository: AdjudicationRepairBuilderRepository,
) {
  fun builder(): AdjudicationRepairBuilder = AdjudicationRepairBuilder(repository)
}

@Component
class AdjudicationRepairBuilderRepository(
  val repairTypeRepository: ReferenceCodeRepository<AdjudicationRepairType>,
) {
  fun lookupRepairType(code: String): AdjudicationRepairType = repairTypeRepository.findByIdOrNull(AdjudicationRepairType.pk(code))!!
}

class AdjudicationRepairBuilder(
  private val repository: AdjudicationRepairBuilderRepository,
) : AdjudicationRepairDsl {
  private lateinit var adjudicationRepair: AdjudicationIncidentRepair

  fun build(
    repairType: String,
    comment: String?,
    repairCost: BigDecimal?,
    incident: AdjudicationIncident,
    repairSequence: Int,
  ): AdjudicationIncidentRepair = AdjudicationIncidentRepair(
    id = AdjudicationIncidentRepairId(incident.id, repairSequence),
    incident = incident,
    comment = comment,
    repairCost = repairCost,
    type = repository.lookupRepairType(repairType),
  )
    .also { adjudicationRepair = it }
}

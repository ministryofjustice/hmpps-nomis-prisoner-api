package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationPleaFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class AdjudicationHearingResultDslMarker

@NomisDataDslMarker
interface AdjudicationHearingResultDsl {
  @AdjudicationHearingResultAwardDslMarker
  fun award(
    statusCode: String,
    sanctionDays: Int? = null,
    sanctionMonths: Int? = null,
    compensationAmount: BigDecimal? = null,
    sanctionCode: String,
    comment: String? = null,
    effectiveDate: LocalDate,
    statusDate: LocalDate? = null,
    consecutiveHearingResultAward: AdjudicationHearingResultAward? = null,
    sanctionIndex: Int? = null,
    dsl: AdjudicationHearingResultAwardDsl.() -> Unit = {},
  ): AdjudicationHearingResultAward
}

@Component
class AdjudicationHearingResultBuilderFactory(
  private val adjudicationHearingResultAwardBuilderFactory: AdjudicationHearingResultAwardBuilderFactory,
  private val repository: AdjudicationHearingResultBuilderRepository,

) {
  fun builder(): AdjudicationHearingResultBuilder {
    return AdjudicationHearingResultBuilder(adjudicationHearingResultAwardBuilderFactory, repository)
  }
}

@Component
class AdjudicationHearingResultBuilderRepository(
  val pleaFindingTypeRepository: ReferenceCodeRepository<AdjudicationPleaFindingType>,
  val findingTypeRepository: ReferenceCodeRepository<AdjudicationFindingType>,
) {
  fun lookupHearingResultPleaType(code: String): AdjudicationPleaFindingType =
    pleaFindingTypeRepository.findByIdOrNull(AdjudicationPleaFindingType.pk(code))!!

  fun lookupHearingResultFindingType(code: String): AdjudicationFindingType =
    findingTypeRepository.findByIdOrNull(AdjudicationFindingType.pk(code))!!
}

class AdjudicationHearingResultBuilder(
  private val adjudicationHearingResultAwardBuilderFactory: AdjudicationHearingResultAwardBuilderFactory,
  private val repository: AdjudicationHearingResultBuilderRepository,
) : AdjudicationHearingResultDsl {
  private lateinit var adjudicationHearingResult: AdjudicationHearingResult

  fun build(
    hearing: AdjudicationHearing,
    charge: AdjudicationIncidentCharge,
    pleaFindingCode: String,
    findingCode: String,
    index: Int,
  ): AdjudicationHearingResult =
    AdjudicationHearingResult(
      id = AdjudicationHearingResultId(hearing.id, index),
      chargeSequence = charge.id.chargeSequence,
      incident = charge.incident,
      hearing = hearing,
      offence = charge.offence,
      incidentCharge = charge,
      pleaFindingType = repository.lookupHearingResultPleaType(pleaFindingCode),
      findingType = repository.lookupHearingResultFindingType(findingCode),
      pleaFindingCode = pleaFindingCode,
      resultAwards = mutableListOf(),
    )
      .also { adjudicationHearingResult = it }

  override fun award(
    statusCode: String,
    sanctionDays: Int?,
    sanctionMonths: Int?,
    compensationAmount: BigDecimal?,
    sanctionCode: String,
    comment: String?,
    effectiveDate: LocalDate,
    statusDate: LocalDate?,
    consecutiveHearingResultAward: AdjudicationHearingResultAward?,
    sanctionIndex: Int?,
    dsl: AdjudicationHearingResultAwardDsl.() -> Unit,
  ) =
    adjudicationHearingResultAwardBuilderFactory.builder().let { builder ->
      builder.build(
        statusCode = statusCode,
        sanctionDays = sanctionDays,
        sanctionMonths = sanctionMonths,
        compensationAmount = compensationAmount,
        sanctionCode = sanctionCode,
        comment = comment,
        effectiveDate = effectiveDate,
        statusDate = statusDate,
        result = adjudicationHearingResult,
        party = adjudicationHearingResult.hearing.hearingParty,
        sanctionIndex = sanctionIndex ?: (adjudicationHearingResult.resultAwards.size + 1),
        consecutiveHearingResultAward = consecutiveHearingResultAward,
      )
        .also { adjudicationHearingResult.resultAwards += it }
        .also { builder.apply(dsl) }
    }
}

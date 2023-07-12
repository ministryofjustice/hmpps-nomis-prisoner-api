package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAwardId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class AdjudicationHearingResultAwardDslMarker

@NomisDataDslMarker
interface AdjudicationHearingResultAwardDsl

@Component
class AdjudicationHearingResultAwardBuilderFactory(
  private val repository: AdjudicationHearingResultAwardBuilderRepository,
) {
  fun builder(): AdjudicationHearingResultAwardBuilder {
    return AdjudicationHearingResultAwardBuilder(repository)
  }
}

@Component
class AdjudicationHearingResultAwardBuilderRepository(
  val sanctionStatusRepository: ReferenceCodeRepository<AdjudicationSanctionStatus>,
  val sanctionTypeRepository: ReferenceCodeRepository<AdjudicationSanctionType>,
) {
  fun lookupSanctionStatus(code: String): AdjudicationSanctionStatus =
    sanctionStatusRepository.findByIdOrNull(AdjudicationSanctionStatus.pk(code))!!

  fun lookupSanctionType(code: String): AdjudicationSanctionType =
    sanctionTypeRepository.findByIdOrNull(AdjudicationSanctionType.pk(code))!!
}

class AdjudicationHearingResultAwardBuilder(
  private val repository: AdjudicationHearingResultAwardBuilderRepository,
) : AdjudicationHearingResultAwardDsl {
  private lateinit var adjudicationHearingResultAward: AdjudicationHearingResultAward

  fun build(
    statusCode: String,
    sanctionDays: Int?,
    sanctionMonths: Int?,
    compensationAmount: BigDecimal?,
    sanctionCode: String,
    comment: String?,
    effectiveDate: LocalDate,
    statusDate: LocalDate?,
    result: AdjudicationHearingResult,
    party: AdjudicationIncidentParty,
    sanctionIndex: Int,
    consecutiveHearingResultAward: AdjudicationHearingResultAward? = null,

  ): AdjudicationHearingResultAward = AdjudicationHearingResultAward(
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
    .also { adjudicationHearingResultAward = it }
}

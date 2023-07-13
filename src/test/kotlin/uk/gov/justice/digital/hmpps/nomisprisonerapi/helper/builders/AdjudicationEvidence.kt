package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class AdjudicationEvidenceDslMarker

@NomisDataDslMarker
interface AdjudicationEvidenceDsl

@Component
class AdjudicationEvidenceBuilderFactory(
  private val repository: AdjudicationEvidenceBuilderRepository,
) {
  fun builder(): AdjudicationEvidenceBuilder {
    return AdjudicationEvidenceBuilder(repository)
  }
}

@Component
class AdjudicationEvidenceBuilderRepository(
  val evidenceTypeRepository: ReferenceCodeRepository<AdjudicationEvidenceType>,
) {
  fun lookupAdjudicationEvidenceType(code: String): AdjudicationEvidenceType =
    evidenceTypeRepository.findByIdOrNull(Pk(AdjudicationEvidenceType.OIC_STMT_TYP, code))!!
}

class AdjudicationEvidenceBuilder(
  private val repository: AdjudicationEvidenceBuilderRepository,
) : AdjudicationEvidenceDsl {
  private lateinit var adjudicationEvidence: AdjudicationEvidence

  fun build(
    detail: String,
    type: String,
    date: LocalDate,
    investigation: AdjudicationInvestigation,
  ): AdjudicationEvidence = AdjudicationEvidence(
    statementDate = date,
    statementDetail = detail,
    statementType = repository.lookupAdjudicationEvidenceType(type),
    investigation = investigation,
  )
    .also { adjudicationEvidence = it }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate

@DslMarker
annotation class AdjudicationInvestigationDslMarker

@NomisDataDslMarker
interface AdjudicationInvestigationDsl {
  @AdjudicationEvidenceDslMarker
  fun evidence(
    detail: String = "Knife found",
    type: String = "WEAP",
    date: LocalDate = LocalDate.now(),
    dsl: AdjudicationEvidenceDsl.() -> Unit = {},
  ): AdjudicationEvidence
}

@Component
class AdjudicationInvestigationBuilderFactory(
  private val adjudicationEvidenceBuilderFactory: AdjudicationEvidenceBuilderFactory,
) {
  fun builder(): AdjudicationInvestigationBuilder = AdjudicationInvestigationBuilder(adjudicationEvidenceBuilderFactory)
}

class AdjudicationInvestigationBuilder(
  private val adjudicationEvidenceBuilderFactory: AdjudicationEvidenceBuilderFactory,
) : AdjudicationInvestigationDsl {
  private lateinit var adjudicationInvestigation: AdjudicationInvestigation

  fun build(
    investigator: Staff,
    comment: String?,
    assignedDate: LocalDate,
    incidentParty: AdjudicationIncidentParty,
  ): AdjudicationInvestigation = AdjudicationInvestigation(
    investigator = investigator,
    assignedDate = assignedDate,
    comment = comment,
    incidentParty = incidentParty,
  )
    .also { adjudicationInvestigation = it }

  override fun evidence(detail: String, type: String, date: LocalDate, dsl: AdjudicationEvidenceDsl.() -> Unit) = adjudicationEvidenceBuilderFactory.builder().let { builder ->
    builder.build(
      detail = detail,
      type = type,
      date = date,
      investigation = adjudicationInvestigation,
    )
      .also { adjudicationInvestigation.evidence += it }
      .also { builder.apply(dsl) }
  }
}

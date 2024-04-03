package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

@DslMarker
annotation class CSIPPlanDslMarker

@NomisDataDslMarker
interface CSIPPlanDsl

@Component
class CSIPPlanBuilderFactory(
  private val repository: CSIPPlanBuilderRepository,

) {
  fun builder(): CSIPPlanBuilder {
    return CSIPPlanBuilder(
      repository,
    )
  }
}

@Component
class CSIPPlanBuilderRepository()

class CSIPPlanBuilder(
  private val repository: CSIPPlanBuilderRepository,
) : CSIPPlanDsl {
  private lateinit var csipPlan: CSIPPlan

  fun build(
    csipReport: CSIPReport,
    identifiedNeed: String,
    intervention: String,
    reportingStaff: Staff,
  ): CSIPPlan =
    CSIPPlan(
      csipReport = csipReport,
      identifiedNeed = identifiedNeed,
      intervention = intervention,
      reportingStaff = reportingStaff,
    )
      .also { csipPlan = it }
}

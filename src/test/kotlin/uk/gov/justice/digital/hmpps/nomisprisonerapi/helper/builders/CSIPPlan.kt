package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDate

@DslMarker
annotation class CSIPPlanDslMarker

@NomisDataDslMarker
interface CSIPPlanDsl

@Component
class CSIPPlanBuilderFactory {
  fun builder() = CSIPPlanBuilder()
}

class CSIPPlanBuilder : CSIPPlanDsl {
  fun build(
    csipReport: CSIPReport,
    identifiedNeed: String,
    intervention: String,
    progression: String?,
    referredBy: String,
    targetDate: LocalDate,
  ): CSIPPlan = CSIPPlan(
    csipReport = csipReport,
    identifiedNeed = identifiedNeed,
    intervention = intervention,
    progression = progression,
    referredBy = referredBy,
    targetDate = targetDate,
  )
}

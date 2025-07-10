package uk.gov.justice.digital.hmpps.nomisprisonerapi.aasentencing

import java.time.LocalDate

data class AdjustmentFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

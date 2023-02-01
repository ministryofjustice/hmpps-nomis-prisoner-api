package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import java.time.LocalDate

data class AdjustmentFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?
)

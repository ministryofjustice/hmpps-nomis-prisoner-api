package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import java.time.LocalDate

data class IncentiveFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val latestOnly: Boolean,
)

package uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter

import java.time.LocalDate

data class IncentiveFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val latestOnly: Boolean,
)

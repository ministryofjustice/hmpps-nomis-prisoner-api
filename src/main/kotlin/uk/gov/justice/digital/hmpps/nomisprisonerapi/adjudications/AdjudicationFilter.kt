package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import java.time.LocalDate

data class AdjudicationFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val prisonIds: List<String>?,
)

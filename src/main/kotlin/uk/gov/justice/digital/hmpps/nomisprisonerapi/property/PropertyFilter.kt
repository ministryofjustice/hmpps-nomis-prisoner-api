package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import java.time.LocalDate

data class PropertyFilter(
  val prisonIds: List<String>,
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

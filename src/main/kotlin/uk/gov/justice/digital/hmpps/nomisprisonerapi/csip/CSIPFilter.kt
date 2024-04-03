package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import java.time.LocalDate

data class CSIPFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

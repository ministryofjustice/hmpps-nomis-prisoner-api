package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import java.time.LocalDateTime

data class CourtCaseFilter(
  val fromDateTime: LocalDateTime?,
  val toDateTime: LocalDateTime?,
)

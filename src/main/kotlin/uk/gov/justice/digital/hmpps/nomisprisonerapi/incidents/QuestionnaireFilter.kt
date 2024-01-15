package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import java.time.LocalDate

data class QuestionnaireFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import java.time.LocalDate

data class NonAssociationFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

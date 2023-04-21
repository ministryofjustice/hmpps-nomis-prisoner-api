package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import java.time.LocalDate

data class AppointmentFilter(
  val prisonIds: List<String>,
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

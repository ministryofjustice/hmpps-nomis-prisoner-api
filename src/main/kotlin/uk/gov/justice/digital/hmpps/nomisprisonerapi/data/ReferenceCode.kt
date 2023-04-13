package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import java.time.LocalDate

data class ReferenceCode(
  val code: String,
  val domain: String,
  val description: String,
  val active: Boolean,
  val sequence: Int?,
  val parentCode: String?,
  val expiredDate: LocalDate? = null,
)

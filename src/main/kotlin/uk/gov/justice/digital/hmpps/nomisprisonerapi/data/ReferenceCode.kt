package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

data class ReferenceCode(
  val code: String,
  val domain: String,
  val description: String,
  val active: Boolean,
  val sequence: Int?,
  val parentCode: String?,
)

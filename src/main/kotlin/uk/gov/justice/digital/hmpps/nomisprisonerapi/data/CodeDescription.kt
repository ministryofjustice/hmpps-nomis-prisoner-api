package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

data class CodeDescription(val code: String, val description: String)

fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.toCodeDescription() = CodeDescription(code, description)
fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation.toCodeDescription() = CodeDescription(id, description)
fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode.toCodeDescription() = CodeDescription(code, description)
fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload?.toCodeDescription() = this?.let { CodeDescription(id, description) }

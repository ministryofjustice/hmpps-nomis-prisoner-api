package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

data class CodeDescription(val code: String, val description: String)

fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.toCodeDescription() = CodeDescription(code, description)

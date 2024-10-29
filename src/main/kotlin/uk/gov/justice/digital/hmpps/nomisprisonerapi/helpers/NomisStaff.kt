package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount

fun List<StaffUserAccount>.usernamePreferringGeneralAccount() =
  this.maxByOrNull { it.type }?.username ?: "unknown"

fun Staff.usernamePreferringGeneralAccount() =
  accounts.usernamePreferringGeneralAccount()

package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount

internal fun findActiveCaseloadId(modified: StaffUserAccount?, created: StaffUserAccount?) = if (modified != null) {
  modified.activeCaseloadId
} else {
  created?.activeCaseloadId
}

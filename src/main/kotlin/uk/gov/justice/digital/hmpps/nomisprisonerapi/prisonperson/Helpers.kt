package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

// NOMIS truncates the time from booking end date, so try and get the accurate time from the last release movement
internal fun OffenderBooking.getReleaseTime(): LocalDateTime? =
  takeIf { !active }
    ?.let {
      externalMovements
        .filter { it.movementType?.code == "REL" }
        .maxByOrNull { it.movementTime }
        ?.movementTime
        ?: bookingEndDate
    }

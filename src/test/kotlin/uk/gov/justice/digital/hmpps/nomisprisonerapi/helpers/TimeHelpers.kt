package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// DATE columns truncate milliseconds, but H2 uses half-up rounding, so we need to do the same with test data
internal fun LocalDateTime.roundToNearestSecond(): LocalDateTime {
  val secondsOnly = this.truncatedTo(ChronoUnit.SECONDS)
  val nanosOnly = this.nano
  val nanosRounded = if (nanosOnly >= 500_000_000) 1 else 0
  return secondsOnly.plusSeconds(nanosRounded.toLong())
}

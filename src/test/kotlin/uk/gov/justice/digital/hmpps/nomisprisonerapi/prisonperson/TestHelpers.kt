package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal fun LocalDateTime.roundToNearestSecond(): LocalDateTime {
  val secondsOnly = this.truncatedTo(ChronoUnit.SECONDS)
  val nanosOnly = this.nano
  val nanosRounded = if (nanosOnly >= 500_000_000) 1 else 0
  return secondsOnly.plusSeconds(nanosRounded.toLong())
}

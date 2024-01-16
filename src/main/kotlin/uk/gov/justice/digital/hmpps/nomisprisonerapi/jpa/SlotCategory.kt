package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.time.LocalTime

/**
 * Enum representing NOMIS reference domain PA_SLOT
 */
enum class SlotCategory {

  AM,
  PM,
  ED,
  ;

  companion object {
    fun of(start: LocalTime): SlotCategory =
      when {
        start.hour < 12 -> AM
        start.hour < 17 -> PM
        else -> ED
      }
  }
}

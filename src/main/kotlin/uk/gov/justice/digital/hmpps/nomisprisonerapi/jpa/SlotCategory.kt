package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.time.LocalTime

enum class SlotCategory {

  AM, PM, ED;

  companion object {
    fun of(start: LocalTime): SlotCategory =
      when {
        start.hour < 12 -> AM
        start.hour < 17 -> PM
        else -> ED
      }
  }
}

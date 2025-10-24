package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import java.time.DayOfWeek

@Service
class VisitsConfigurationService(val agencyVisitTimeRepository: AgencyVisitTimeRepository) {
  fun getVisitTimeSlotIds(pageRequest: Pageable): Page<VisitTimeSlotIdResponse> = agencyVisitTimeRepository.findAllIds(pageRequest).map {
    VisitTimeSlotIdResponse(
      prisonId = it.prisonId,
      dayOfWeek = it.weekdayCode.asDayOfWeek(),
      timeSlotSequence = it.timeSlotSequence,
    )
  }
}

private fun String.asDayOfWeek(): DayOfWeek = when (this) {
  "MON" -> DayOfWeek.MONDAY
  "TUE" -> DayOfWeek.TUESDAY
  "WED" -> DayOfWeek.WEDNESDAY
  "THU" -> DayOfWeek.THURSDAY
  "FRI" -> DayOfWeek.FRIDAY
  "SAT" -> DayOfWeek.SATURDAY
  "SUN" -> DayOfWeek.SUNDAY
  else -> throw IllegalArgumentException("Invalid day of week $this")
}

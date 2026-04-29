package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CourtScheduleService {
  // TODO get COURT_EVENTS record and transform to CourtScheduleOut
  fun getCourtScheduleOut(offenderNo: String, eventId: Long) = CourtScheduleOut(
    1,
    1,
    LocalDate.now(),
    LocalDateTime.now(),
    "any",
    "any",
    "any",
    "any",
  )
}

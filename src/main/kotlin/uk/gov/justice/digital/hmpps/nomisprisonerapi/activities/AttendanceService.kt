package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAttendanceRequest

@Service
class AttendanceService {

  fun createAttendance(scheduleId: Long, bookingId: Long, createAttendanceRequest: CreateAttendanceRequest) {
    // TODO SDIT-689 Implement this service
  }
}

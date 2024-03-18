package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CourseAttendanceDslMarker

@NomisDataDslMarker
interface CourseAttendanceDsl

@Component
class CourseAttendanceBuilderRepository(
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val agencyLocationRepository: AgencyLocationRepository? = null,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType>? = null,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository?.findByIdOrNull(id)!!
  fun agencyInternalLocation(locationId: Long) = agencyInternalLocationRepository.findByIdOrNull(locationId)
  fun eventStatus(code: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun attendanceOutcome(code: String) = attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(code))!!
}

@Component
class CourseAttendanceBuilderFactory(private val repository: CourseAttendanceBuilderRepository? = null) {
  fun builder() = CourseAttendanceBuilder(repository)
}

class CourseAttendanceBuilder(
  private val repository: CourseAttendanceBuilderRepository? = null,
) : CourseAttendanceDsl {

  fun build(
    courseAllocation: OffenderProgramProfile,
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    pay: Boolean?,
    paidTransactionId: Long?,
  ): OffenderCourseAttendance =
    OffenderCourseAttendance(
      eventId = eventId,
      offenderBooking = courseAllocation.offenderBooking,
      eventDate = courseSchedule.scheduleDate,
      startTime = courseSchedule.startTime,
      endTime = courseSchedule.endTime,
      eventStatus = eventStatus(eventStatusCode),
      toInternalLocation = toInternalLocationId?.let {
        agencyInternalLocation(
          toInternalLocationId,
          "CLAS",
          lookupAgency("LEI"),
        )
      },
      courseSchedule = courseSchedule,
      attendanceOutcome = outcomeReasonCode?.let { attendanceOutcome(outcomeReasonCode) },
      offenderProgramProfile = courseAllocation,
      inTime = courseSchedule.startTime,
      outTime = courseSchedule.endTime,
      courseActivity = courseSchedule.courseActivity,
      prison = courseSchedule.courseActivity.prison,
      program = courseSchedule.courseActivity.program,
      pay = pay,
      paidTransactionId = paidTransactionId,
    )

  private fun lookupAgency(id: String): AgencyLocation = repository?.lookupAgency(id)
    ?: AgencyLocation(id = id, description = id, type = AgencyLocationType.PRISON_TYPE, active = true)

  private fun eventStatus(code: String) = repository?.eventStatus(code)
    ?: EventStatus(code = code, description = code)

  private fun agencyInternalLocation(
    id: Long,
    locationType: String,
    prison: AgencyLocation,
  ) = repository?.agencyInternalLocation(id)
    ?: AgencyInternalLocation(
      locationId = id,
      active = true,
      locationType = locationType,
      agency = prison,
      description = "Classroom 1",
      locationCode = id.toString(),
    )

  private fun attendanceOutcome(code: String) = repository?.attendanceOutcome(code)
    ?: AttendanceOutcome(code = code, description = code)
}

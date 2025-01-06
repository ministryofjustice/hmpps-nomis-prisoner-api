package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CourseAttendanceDslMarker

@NomisDataDslMarker
interface CourseAttendanceDsl {
  @OffenderTransactionDslMarker
  fun transaction(
    transactionId: Long = 0,
    entrySeq: Long = 1,
    dsl: OffenderTransactionDsl.() -> Unit = {},
  ): OffenderTransaction
}

@Component
class CourseAttendanceBuilderRepository(
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val agencyLocationRepository: AgencyLocationRepository? = null,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository?.findByIdOrNull(id)!!
  fun agencyInternalLocation(locationId: Long) = agencyInternalLocationRepository.findByIdOrNull(locationId)
  fun eventStatus(code: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun attendanceOutcome(code: String) = attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(code))!!
}

@Component
class CourseAttendanceBuilderFactory(
  private val repository: CourseAttendanceBuilderRepository? = null,
  private val offenderTransactionBuilderFactory: OffenderTransactionBuilderFactory,
) {
  fun builder() = CourseAttendanceBuilder(repository, offenderTransactionBuilderFactory)
}

class CourseAttendanceBuilder(
  private val repository: CourseAttendanceBuilderRepository? = null,
  private val offenderTransactionBuilderFactory: OffenderTransactionBuilderFactory,
) : CourseAttendanceDsl {

  private lateinit var courseAttendance: OffenderCourseAttendance

  fun build(
    courseAllocation: OffenderProgramProfile,
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    pay: Boolean?,
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
    )
      .also { courseAttendance = it }

  override fun transaction(
    transactionId: Long,
    entrySeq: Long,
    dsl: OffenderTransactionDsl.() -> Unit,
  ): OffenderTransaction =
    offenderTransactionBuilderFactory.builder().let { builder ->
      builder.build(
        offenderCourseAttendance = courseAttendance,
        transactionId = transactionId,
        entrySeq = entrySeq,
        caseloadId = courseAttendance.courseActivity.caseloadId,
        offenderId = courseAttendance.offenderBooking.offender.id,
      )
        .also { courseAttendance.transaction = it }
        .also { builder.apply(dsl) }
    }

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

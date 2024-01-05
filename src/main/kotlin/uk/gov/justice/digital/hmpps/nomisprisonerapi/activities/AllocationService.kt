package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.AllocationExclusion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.AllocationReconciliationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.FindActiveAllocationIdsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.FindSuspendedAllocationsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderActivityExclusion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBandId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class AllocationService(
  private val scheduleRuleService: ScheduleRuleService,
  private val activityRepository: ActivityRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
  private val offenderProgramStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>,
  private val programServiceEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun upsertAllocation(courseActivityId: Long, request: UpsertAllocationRequest): UpsertAllocationResponse {
    val allocation = toOffenderProgramProfile(courseActivityId, request)
    val created = allocation.offenderProgramReferenceId == 0L
    return offenderProgramProfileRepository.save(allocation)
      .let { UpsertAllocationResponse(it.offenderProgramReferenceId, created, it.prison?.id ?: "") }
      .also {
        telemetryClient.trackEvent(
          "activity-allocation-${if (it.created) "created" else "updated"}",
          mapOf(
            "nomisCourseActivityId" to courseActivityId.toString(),
            "nomisAllocationId" to it.offenderProgramReferenceId.toString(),
            "bookingId" to allocation.offenderBooking.bookingId.toString(),
            "offenderNo" to allocation.offenderBooking.offender.nomsId,
            "prisonId" to allocation.prison?.id,
          ),
          null,
        )
      }
  }

  fun findActiveAllocations(pageRequest: Pageable, prisonId: String, excludeProgramCodes: List<String>?, courseActivityId: Long?): Page<FindActiveAllocationIdsResponse> {
    val excludePrograms = excludeProgramCodes?.takeIf { it.isNotEmpty() } ?: listOf(" ") // for unknown reasons the SQL fails on Oracle with an empty list or a zero length string
    return findPrisonOrThrow(prisonId)
      .let { offenderProgramProfileRepository.findActiveAllocations(prisonId, excludePrograms, courseActivityId, pageRequest) }
      .map { FindActiveAllocationIdsResponse(it) }
  }

  fun findSuspendedAllocations(prisonId: String, excludeProgramCodes: List<String>?, courseActivityId: Long?): List<FindSuspendedAllocationsResponse> {
    val excludePrograms = excludeProgramCodes?.takeIf { it.isNotEmpty() } ?: listOf(" ") // for unknown reasons the SQL fails on Oracle with an empty list or a zero length string
    return findPrisonOrThrow(prisonId)
      .let { offenderProgramProfileRepository.findSuspendedAllocations(prisonId, excludePrograms, courseActivityId) }
      .map {
        FindSuspendedAllocationsResponse(
          offenderNo = it.getNomsId(),
          courseActivityId = it.getCourseActivityId(),
          courseActivityDescription = it.getCourseActivityDescription(),
        )
      }
  }

  fun getAllocation(allocationId: Long): GetAllocationResponse =
    offenderProgramProfileRepository.findByIdOrNull(allocationId)
      ?.let {
        GetAllocationResponse(
          prisonId = it.prison!!.id,
          courseActivityId = it.courseActivity.courseActivityId,
          nomisId = it.offenderBooking.offender.nomsId,
          bookingId = it.offenderBooking.bookingId,
          startDate = it.startDate,
          endDate = it.endDate,
          endComment = it.endComment,
          endReasonCode = it.endReason?.code,
          suspended = it.suspended,
          payBand = it.payBands.firstOrNull(OffenderProgramProfilePayBand::isActive)?.payBand?.code,
          livingUnitDescription = it.offenderBooking.assignedLivingUnit?.description,
          exclusions = it.offenderExclusions.map { exclusion ->
            AllocationExclusion(
              day = exclusion.excludeDay.name,
              slot = exclusion.slotCategory?.name,
            )
          },
          scheduleRules = scheduleRuleService.mapRules(it.courseActivity.courseScheduleRules),
        )
      }
      ?: throw NotFoundException("Offender program profile with id=$allocationId does not exist")

  private fun toOffenderProgramProfile(courseActivityId: Long, request: UpsertAllocationRequest): OffenderProgramProfile {
    val existingAllocation =
      offenderProgramProfileRepository.findByCourseActivityCourseActivityIdAndOffenderBookingBookingIdAndProgramStatusCode(
        courseActivityId,
        request.bookingId,
        "ALLOC",
      )

    val requestedPayBand = findPayBandOrThrow(request.payBandCode)

    val offenderBooking = findOffenderBookingOrThrow(request.bookingId)

    val courseActivity = findCourseActivityOrThrow(courseActivityId, offenderBooking, requestedPayBand, newAllocation = existingAllocation == null)

    val requestedEndReason = findEndReasonOrThrow(request.endReason)

    val allocation = existingAllocation
      ?: newAllocation(request, offenderBooking, courseActivity, requestedPayBand)

    return allocation.apply {
      startDate = if (allocation.startDate > LocalDate.now()) request.startDate else allocation.startDate
      endDate = request.endDate
      endReason = requestedEndReason
      suspended = request.suspended ?: false
      endComment = updateEndComment(request)
      updatePayBands(requestedPayBand, request)
      programStatus = findProgramStatus(request.programStatusCode)
      updateExclusions(request.exclusions ?: listOf())
    }
  }

  private fun findProgramStatus(programStatusCode: String): OffenderProgramStatus =
    offenderProgramStatusRepository.findByIdOrNull(OffenderProgramStatus.pk(programStatusCode))
      ?: throw BadDataException("Program status code=$programStatusCode does not exist")

  private fun findEndReasonOrThrow(endReason: String?): ProgramServiceEndReason? =
    endReason?.let {
      programServiceEndReasonRepository.findByIdOrNull(ProgramServiceEndReason.pk(it))
        ?: throw BadDataException("End reason code=$endReason does not exist")
    }

  private fun findCourseActivityOrThrow(
    courseActivityId: Long,
    offenderBooking: OffenderBooking,
    requestedPayBand: PayBand?,
    newAllocation: Boolean,
  ): CourseActivity {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    if (courseActivity.prison.id != offenderBooking.location?.id && newAllocation) {
      throw BadDataException("Prisoner is at prison=${offenderBooking.location?.id}, not the Course activity prison=${courseActivity.prison.id}")
    }

    requestedPayBand?.run {
      if (courseActivity.payRates.find { it.payBand.code == requestedPayBand.code } == null) {
        throw BadDataException("Pay band code ${requestedPayBand.code} does not exist for course activity with id=$courseActivityId")
      }
    }

    return courseActivity
  }

  private fun findOffenderBookingOrThrow(bookingId: Long): OffenderBooking =
    offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw BadDataException("Booking with id=$bookingId does not exist")

  private fun findPayBandOrThrow(payBandCode: String?): PayBand? =
    payBandCode?.let {
      payBandRepository.findByIdOrNull(PayBand.pk(payBandCode))
        ?: throw BadDataException("Pay band code $payBandCode does not exist")
    }

  private fun OffenderProgramProfile.updatePayBands(requestedPayBand: PayBand?, request: UpsertAllocationRequest) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val expiredPayBand = payBands.filter { it.endDate != null }.filter { it.endDate!! < today }.maxByOrNull { it.id.startDate }
    val activePayBand = payBands.firstOrNull { it.id.startDate <= today && (it.endDate == null || it.endDate!! >= today) }
    val futurePayBand = payBands.firstOrNull { it.id.startDate > today }
    when {
      // if no active pay band then add a new pay band from today (or the date requested if in the future)
      activePayBand == null && futurePayBand == null -> {
        requestedPayBand?.also {
          payBands.add(payBand(requestedPayBand, listOf(today, request.startDate).max(), request.endDate))
        }
      }
      // if an active pay band is changed then it is end dated to today and the new pay band becomes effective tomorrow
      activePayBand != null && futurePayBand == null && (requestedPayBand == null || requestedPayBand.code != activePayBand.payBand.code) -> {
        activePayBand.endDate = today
        requestedPayBand?.also {
          payBands.add(payBand(requestedPayBand, tomorrow, request.endDate))
        }
      }
      // if the active pay band is not changed then the only thing that can be updated is the end date
      activePayBand != null && futurePayBand == null -> {
        activePayBand.endDate = request.endDate
      }
      // pay bands not yet active should be replaced with the new request
      expiredPayBand == null && activePayBand == null && futurePayBand != null -> {
        payBands.remove(futurePayBand)
        requestedPayBand?.also {
          payBands.add(payBand(requestedPayBand, request.startDate, request.endDate))
        }
      }
      // pay bands not yet active with an existing expired/active pay band can update everything apart from their start date
      futurePayBand != null -> {
        if (requestedPayBand == null) {
          payBands.remove(futurePayBand)
        } else {
          futurePayBand.payBand = requestedPayBand
          futurePayBand.endDate = request.endDate
        }
      }
    }
  }

  private fun OffenderProgramProfile.payBand(payBand: PayBand, startDate: LocalDate, endDate: LocalDate?) =
    OffenderProgramProfilePayBand(
      id = OffenderProgramProfilePayBandId(
        offenderProgramProfile = this,
        startDate = startDate,
      ),
      payBand = payBand,
      endDate = endDate,
    )

  private fun OffenderProgramProfile.updateExclusions(requestedExclusions: List<AllocationExclusion>) {
    requestedExclusions
      .map { requestedExclusion -> findOrCreateExclusion(requestedExclusion) }
      .also {
        offenderExclusions.clear()
        offenderExclusions.addAll(it)
      }
  }

  private fun OffenderProgramProfile.findOrCreateExclusion(requestedExclusion: AllocationExclusion) =
    offenderExclusions.find { existing ->
      requestedExclusion.day == existing.excludeDay.name && requestedExclusion.slot == existing.slotCategory?.name
    }
      ?: OffenderActivityExclusion(
        offenderBooking = offenderBooking,
        courseActivity = courseActivity,
        offenderProgramProfile = this,
        excludeDay = findExcludeDay(requestedExclusion.day),
        slotCategory = requestedExclusion.slot?.let { findSlotCategory(requestedExclusion.slot) },
      )

  private fun findExcludeDay(day: String): WeekDay =
    WeekDay.entries.find { it.name == day }
      ?: throw BadDataException("Exclusion day $day does not exist")

  private fun findSlotCategory(slot: String): SlotCategory =
    SlotCategory.entries.find { it.name == slot }
      ?: throw BadDataException("Exclusion slot $slot does not exist")

  private fun updateEndComment(request: UpsertAllocationRequest) =
    if (request.endDate != null) {
      request.endComment
    } else if (request.suspended == true) {
      request.suspendedComment
    } else {
      null
    }

  private fun newAllocation(
    request: UpsertAllocationRequest,
    offenderBooking: OffenderBooking,
    courseActivity: CourseActivity,
    payBand: PayBand?,
  ) =
    OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = request.startDate,
      programStatus = offenderProgramStatusRepository.findById(OffenderProgramStatus.pk("ALLOC")).get(),
      courseActivity = courseActivity,
      prison = courseActivity.prison,
    )
      .apply {
        payBand?.also {
          payBands.add(
            OffenderProgramProfilePayBand(
              id = OffenderProgramProfilePayBandId(
                offenderProgramProfile = this,
                startDate = request.startDate,
              ),
              payBand = payBand,
            ),
          )
        }
      }

  fun deleteAllocation(referenceId: Long) = offenderProgramProfileRepository.deleteById(referenceId)

  private fun findPrisonOrThrow(prisonId: String) =
    agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw BadDataException("Prison with id=$prisonId does not exist")

  fun endAllocation(courseAllocation: OffenderProgramProfile, date: LocalDate, endAllocationComment: String? = null) {
    courseAllocation.endDate = date
    courseAllocation.endReason = findEndReasonOrThrow("OTH")
    courseAllocation.programStatus = findProgramStatus("END")
    endAllocationComment?.run { courseAllocation.endComment = endAllocationComment }
  }

  fun endAllocations(courseActivityIds: Collection<Long>, date: LocalDate) {
    offenderProgramProfileRepository.endAllocations(courseActivityIds, date)
  }

  fun findActiveAllocationsSummary(prisonId: String): AllocationReconciliationResponse =
    offenderProgramProfileRepository.findBookingAllocationCountsByPrisonAndPrisonerStatus(prisonId, "ALLOC")
      .let {
        AllocationReconciliationResponse(
          prisonId = prisonId,
          bookings = it,
        )
      }
}

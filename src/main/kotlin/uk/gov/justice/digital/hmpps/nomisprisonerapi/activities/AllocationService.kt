package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBandId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class AllocationService(
  private val activityRepository: ActivityRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
  private val offenderProgramStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>,
  private val programServiceEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason>,
  private val telemetryClient: TelemetryClient,
) {
  fun upsertAllocation(courseActivityId: Long, request: UpsertAllocationRequest): UpsertAllocationResponse {
    val allocation = toOffenderProgramProfile(courseActivityId, request)
    val created = allocation.offenderProgramReferenceId == 0L
    return offenderProgramProfileRepository.save(allocation)
      .let { UpsertAllocationResponse(it.offenderProgramReferenceId, created) }
      .also {
        telemetryClient.trackEvent(
          "activity-allocation-${if (it.created) "created" else "updated"}",
          mapOf(
            "nomisCourseActivityId" to courseActivityId.toString(),
            "nomisAllocationId" to it.offenderProgramReferenceId.toString(),
            "bookingId" to allocation.offenderBooking.bookingId.toString(),
            "offenderNo" to allocation.offenderBooking.offender.nomsId,
          ),
          null,
        )
      }
  }

  private fun toOffenderProgramProfile(courseActivityId: Long, request: UpsertAllocationRequest): OffenderProgramProfile {
    val existingAllocation =
      offenderProgramProfileRepository.findByCourseActivityCourseActivityIdAndOffenderBookingBookingIdAndProgramStatusCode(
        courseActivityId,
        request.bookingId,
        "ALLOC",
      )

    val requestedPayBand = findPayBandOrThrow(request.payBandCode)

    val offenderBooking = findOffenderBookingOrThrow(request.bookingId)

    val courseActivity = findCourseActivityOrThrow(courseActivityId, offenderBooking, requestedPayBand)

    val requestedEndReason = findEndReasonOrThrow(request)

    val allocation = existingAllocation
      ?: newAllocation(request, offenderBooking, courseActivity, requestedPayBand)

    return allocation.apply {
      endDate = request.endDate
      endReason = requestedEndReason
      suspended = request.suspended ?: false
      endComment = updateEndComment(request)
      updatePayBands(requestedPayBand)
      programStatus = findProgramStatus(request.endDate)
    }
  }

  private fun findProgramStatus(endDate: LocalDate?): OffenderProgramStatus {
    val statusCode = if (endDate == null) "ALLOC" else "END"
    return offenderProgramStatusRepository.findById(OffenderProgramStatus.pk(statusCode)).get()
  }

  private fun findEndReasonOrThrow(request: UpsertAllocationRequest): ProgramServiceEndReason? =
    request.endReason?.let {
      programServiceEndReasonRepository.findByIdOrNull(ProgramServiceEndReason.pk(it))
        ?: throw BadDataException("End reason code=${request.endReason} does not exist")
    }

  private fun findCourseActivityOrThrow(
    courseActivityId: Long,
    offenderBooking: OffenderBooking,
    requestedPayBand: PayBand,
  ): CourseActivity {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    if (courseActivity.prison.id != offenderBooking.location?.id) {
      throw BadDataException("Prisoner is at prison=${offenderBooking.location?.id}, not the Course activity prison=${courseActivity.prison.id}")
    }

    if (courseActivity.scheduleEndDate?.isBefore(LocalDate.now()) == true) {
      throw BadDataException("Course activity with id=$courseActivityId has expired")
    }

    if (courseActivity.payRates.find { it.payBand.code == requestedPayBand.code } == null) {
      throw BadDataException("Pay band code ${requestedPayBand.code} does not exist for course activity with id=$courseActivityId")
    }

    return courseActivity
  }

  private fun findOffenderBookingOrThrow(bookingId: Long): OffenderBooking =
    offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw BadDataException("Booking with id=$bookingId does not exist")

  private fun findPayBandOrThrow(payBandCode: String): PayBand =
    payBandRepository.findByIdOrNull(PayBand.pk(payBandCode))
      ?: throw BadDataException("Pay band code $payBandCode does not exist")

  private fun OffenderProgramProfile.updatePayBands(requestedPayBand: PayBand) {
    val activePayBand = payBands.first { it.endDate == null }
    // if we have requested a change to the pay band
    if (activePayBand.payBand.code != requestedPayBand.code) {
      val payBandEndingToday = payBands.find { it.endDate == LocalDate.now() }
      // reinstate the pay band that is ending today
      if (requestedPayBand.code == payBandEndingToday?.payBand?.code) {
        payBands.remove(activePayBand)
        payBandEndingToday.endDate = null
      }
      // update the active pay band that hasn't started yet
      else if (activePayBand.id.startDate > LocalDate.now()) {
        activePayBand.payBand = requestedPayBand
      }
      // end the active pay band and add a new pay band starting tomorrow
      else {
        activePayBand.endDate = LocalDate.now()
        payBands.add(
          OffenderProgramProfilePayBand(
            id = OffenderProgramProfilePayBandId(
              offenderProgramProfile = this,
              startDate = LocalDate.now().plusDays(1),
            ),
            payBand = requestedPayBand,
          ),
        )
      }
    }
  }

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
    payBand: PayBand,
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

  fun deleteAllocation(referenceId: Long) = offenderProgramProfileRepository.deleteById(referenceId)
}

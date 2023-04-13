package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
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
  fun createAllocation(
    courseActivityId: Long,
    dto: CreateAllocationRequest,
  ): CreateAllocationResponse =

    offenderProgramProfileRepository.save(mapOffenderProgramProfileModel(courseActivityId, dto)).run {
      telemetryClient.trackEvent(
        "offender-program-profile-created",
        mapOf(
          "id" to offenderProgramReferenceId.toString(),
          "courseActivityId" to courseActivity?.courseActivityId.toString(),
          "bookingId" to offenderBooking.bookingId.toString(),
        ),
        null,
      )
      CreateAllocationResponse(offenderProgramReferenceId)
    }

  fun updateAllocation(
    courseActivityId: Long,
    dto: UpdateAllocationRequest,
  ): CreateAllocationResponse =

    offenderProgramProfileRepository.findByCourseActivityCourseActivityIdAndOffenderBookingBookingIdAndProgramStatusCode(
      courseActivityId,
      dto.bookingId,
      "ALLOC",
    )
      ?.run {
        endReason =
          dto.endReason?.let { programServiceEndReasonRepository.findByIdOrNull(ProgramServiceEndReason.pk(it)) }
            ?: throw BadDataException("End reason code=${dto.endReason} is invalid")
        endDate = dto.endDate
        endComment = dto.endComment

        telemetryClient.trackEvent(
          "offender-program-profile-ended",
          mapOf(
            "id" to offenderProgramReferenceId.toString(),
            "courseActivityId" to courseActivity?.courseActivityId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
          ),
          null,
        )
        CreateAllocationResponse(offenderProgramReferenceId)
      }

      ?: run {
        activityRepository.findByIdOrNull(courseActivityId)
          ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")
        offenderBookingRepository.findByIdOrNull(dto.bookingId)
          ?: throw NotFoundException("Booking with id=${dto.bookingId} does not exist")
        throw BadDataException("Offender Program Profile with courseActivityId=$courseActivityId and bookingId=${dto.bookingId} and status=ALLOC does not exist")
      }

  private fun mapOffenderProgramProfileModel(
    courseActivityId: Long,
    dto: CreateAllocationRequest,
  ): OffenderProgramProfile {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    val offenderBooking = offenderBookingRepository.findByIdOrNull(dto.bookingId)
      ?: throw BadDataException("Booking with id=${dto.bookingId} does not exist")

    val payBand = payBandRepository.findByIdOrNull(PayBand.pk(dto.payBandCode))
      ?: throw BadDataException("Pay band code ${dto.payBandCode} does not exist")

    offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseActivity, offenderBooking)?.run {
      throw BadDataException("Offender Program Profile with courseActivityId=$courseActivityId and bookingId=${dto.bookingId} already exists")
    }

    if (courseActivity.prison.id != offenderBooking.location?.id) {
      throw BadDataException("Prisoner is at prison=${offenderBooking.location?.id}, not the Course activity prison=${courseActivity.prison.id}")
    }

    if (courseActivity.scheduleEndDate?.isBefore(LocalDate.now()) == true) {
      throw BadDataException("Course activity with id=$courseActivityId has expired")
    }

    if (courseActivity.payRates.find { it.payBand == payBand } == null) {
      throw BadDataException("Pay band code ${payBand.code} does not exist for course activity with id=$courseActivityId")
    }

    return OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = dto.startDate,
      programStatus = offenderProgramStatusRepository.findByIdOrNull(OffenderProgramStatus.pk("ALLOC"))!!,
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = dto.endDate,
    ).apply {
      payBands.add(
        OffenderProgramProfilePayBand(
          offenderProgramProfile = this,
          startDate = dto.startDate,
          payBand = payBand,
          endDate = dto.endDate,
        ),
      )
    }
  }
}

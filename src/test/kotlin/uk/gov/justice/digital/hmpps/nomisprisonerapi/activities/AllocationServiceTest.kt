package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

private const val COURSE_ACTIVITY_ID = 1L
private const val PRISON_ID = "LEI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "TEST"
private const val OFFENDER_BOOKING_ID = -9L
private const val OFFENDER_NO = "A1234AA"
private const val PRISON_DESCRIPTION = "Leeds"
private const val OFFENDER_PROGRAM_REFERENCE_ID = 12345L
private const val PAY_BAND_CODE = "5"

class AllocationServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository = mock()
  private val payBandRepository: ReferenceCodeRepository<PayBand> = mock()
  private val offenderProgramStatusRepository: ReferenceCodeRepository<OffenderProgramStatus> = mock()
  private val programServiceEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason> = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val allocationService = AllocationService(
    activityRepository,
    offenderBookingRepository,
    offenderProgramProfileRepository,
    payBandRepository,
    offenderProgramStatusRepository,
    programServiceEndReasonRepository,
    telemetryClient,
  )

  private val defaultPrison = AgencyLocation(PRISON_ID, PRISON_DESCRIPTION)
  private val defaultProgramService = ProgramService(
    programCode = PROGRAM_CODE,
    description = "desc",
    active = true,
  )
  private val defaultRoom = AgencyInternalLocation(
    agencyId = PRISON_ID,
    description = PRISON_DESCRIPTION,
    locationType = "ROOM",
    locationCode = "ROOM-1",
    locationId = ROOM_ID,
  )

  private fun defaultIepLevel(code: String) = IEPLevel(code, "$code-desc")

  private fun defaultPayBand(code: String) = PayBand(code, "Pay band $code")

  private val defaultOffender = Offender(
    nomsId = OFFENDER_NO,
    lastName = "Smith",
    gender = Gender("MALE", "Male"),
  )
  private val defaultOffenderBooking = OffenderBooking(
    bookingId = OFFENDER_BOOKING_ID,
    offender = defaultOffender,
    location = defaultPrison,
    bookingBeginDate = LocalDateTime.now(),
  )
  private val defaultCourseActivity = CourseActivity(
    courseActivityId = COURSE_ACTIVITY_ID,
    prison = defaultPrison,
    program = defaultProgramService,
    scheduleStartDate = LocalDate.parse("2022-10-31"),
    iepLevel = defaultIepLevel("STD"),
    internalLocation = defaultRoom,
  ).apply {
    payRates.add(
      CourseActivityPayRate(
        id = CourseActivityPayRateId(
          courseActivity = this,
          startDate = LocalDate.parse("2022-11-01"),
          payBandCode = PAY_BAND_CODE,
          iepLevelCode = "ENH",
        ),
        payBand = defaultPayBand(PAY_BAND_CODE),
        iepLevel = defaultIepLevel("ENH"),
        endDate = LocalDate.parse("2022-11-03"),
        halfDayRate = BigDecimal("0.50"),
      ),
    )
  }
  private val createRequest = CreateAllocationRequest(
    bookingId = OFFENDER_BOOKING_ID,
    startDate = LocalDate.parse("2022-10-31"),
    endDate = LocalDate.parse("2022-11-30"),
    payBandCode = PAY_BAND_CODE,
  )

  @Nested
  internal inner class CreateOffenderProgramProfile {

    @BeforeEach
    fun setup() {
      whenever(activityRepository.findById(COURSE_ACTIVITY_ID)).thenReturn(
        Optional.of(defaultCourseActivity),
      )
      whenever(offenderBookingRepository.findById(OFFENDER_BOOKING_ID)).thenReturn(
        Optional.of(defaultOffenderBooking),
      )

      whenever(offenderProgramProfileRepository.save(any())).thenAnswer {
        (it.arguments[0] as OffenderProgramProfile).copy(offenderProgramReferenceId = OFFENDER_PROGRAM_REFERENCE_ID)
      }

      whenever(payBandRepository.findById(PayBand.pk(PAY_BAND_CODE))).thenReturn(
        Optional.of(
          defaultPayBand(
            PAY_BAND_CODE,
          ),
        ),
      )

      whenever(offenderProgramStatusRepository.findById(OffenderProgramStatus.pk("ALLOC"))).thenReturn(
        Optional.of(OffenderProgramStatus("ALLOC", "Allocated")),
      )
    }

    @Test
    fun `Data is mapped correctly`() {
      Assertions.assertThat(allocationService.createAllocation(COURSE_ACTIVITY_ID, createRequest))
        .isEqualTo(CreateAllocationResponse(OFFENDER_PROGRAM_REFERENCE_ID))

      verify(offenderProgramProfileRepository).save(
        org.mockito.kotlin.check {
          with(it) outer@{
            Assertions.assertThat(offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
            Assertions.assertThat(offenderBooking.bookingId).isEqualTo(OFFENDER_BOOKING_ID)
            Assertions.assertThat(program.programCode).isEqualTo(PROGRAM_CODE)
            Assertions.assertThat(startDate).isEqualTo(LocalDate.parse("2022-10-31"))
            Assertions.assertThat(programStatus.code).isEqualTo("ALLOC")
            Assertions.assertThat(courseActivity?.courseActivityId).isEqualTo(COURSE_ACTIVITY_ID)
            Assertions.assertThat(prison?.id).isEqualTo(PRISON_ID)
            Assertions.assertThat(endDate).isEqualTo(LocalDate.parse("2022-11-30"))
            Assertions.assertThat(payBands).hasSize(1)
            with(payBands.first()) {
              Assertions.assertThat(offenderProgramProfile).isSameAs(this@outer)
              Assertions.assertThat(startDate).isSameAs(this@outer.startDate)
              Assertions.assertThat(endDate).isSameAs(this@outer.endDate)
              Assertions.assertThat(payBand.code).isEqualTo(PAY_BAND_CODE)
            }
          }
        },
      )
    }

    @Test
    fun courseActivityNotFound() {
      whenever(activityRepository.findById(COURSE_ACTIVITY_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<NotFoundException>() {
        allocationService.createAllocation(COURSE_ACTIVITY_ID, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Course activity with id=$COURSE_ACTIVITY_ID does not exist")
    }

    @Test
    fun invalidBooking() {
      whenever(offenderBookingRepository.findById(OFFENDER_BOOKING_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        allocationService.createAllocation(COURSE_ACTIVITY_ID, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Booking with id=$OFFENDER_BOOKING_ID does not exist")
    }

    @Test
    fun invalidPayBandCode() {
      val thrown = assertThrows<BadDataException>() {
        allocationService.createAllocation(
          COURSE_ACTIVITY_ID,
          createRequest.copy(payBandCode = "doesnotexist"),
        )
      }
      Assertions.assertThat(thrown.message).isEqualTo("Pay band code doesnotexist does not exist")
    }

    @Test
    fun invalidPayBandCodeForCourse() {
      // We are testing that the pay band is not available for the course, not that the pay band does not exist at all
      whenever(payBandRepository.findById(PayBand.pk("not_on_course"))).thenReturn(Optional.of(defaultPayBand("not_on_course")))

      val thrown = assertThrows<BadDataException>() {
        allocationService.createAllocation(
          COURSE_ACTIVITY_ID,
          createRequest.copy(payBandCode = "not_on_course"),
        )
      }
      Assertions.assertThat(thrown.message).isEqualTo("Pay band code not_on_course does not exist for course activity with id=$COURSE_ACTIVITY_ID")
    }
  }
}

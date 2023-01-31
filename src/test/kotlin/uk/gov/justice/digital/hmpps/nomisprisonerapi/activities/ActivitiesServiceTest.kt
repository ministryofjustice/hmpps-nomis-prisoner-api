package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

private const val courseActivityId = 1L
private const val prisonId = "LEI"
private const val roomId: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val programCode = "TEST"
private const val iepLevel = "STD"
private const val offenderBookingId = -9L
private const val offenderNo = "A1234AA"
private const val prisonDescription = "Leeds"
private const val offenderProgramReferenceId = 12345L
private const val payBandCode = "5"
internal class ActivitiesServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val programServiceRepository: ProgramServiceRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository = mock()
  private val payBandRepository: ReferenceCodeRepository<PayBand> = mock()
  private val programServiceEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason> = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val activitiesService = ActivitiesService(
    activityRepository,
    agencyLocationRepository,
    agencyInternalLocationRepository,
    programServiceRepository,
    availablePrisonIepLevelRepository,
    offenderBookingRepository,
    offenderProgramProfileRepository,
    payBandRepository,
    programServiceEndReasonRepository,
    telemetryClient,
  )

  val defaultPrison = AgencyLocation(prisonId, prisonDescription)
  val defaultProgramService = ProgramService(
    programCode = programCode,
    description = "desc",
    active = true,
  )
  val defaultRoom = AgencyInternalLocation(
    agencyId = prisonId,
    description = prisonDescription,
    locationType = "ROOM",
    locationCode = "ROOM-1",
    locationId = roomId
  )

  fun defaultIepLevel(code: String) = IEPLevel(code, "$code-desc")

  fun defaultPayBand(code: String) = PayBand(code, "Pay band $code")

  @BeforeEach
  fun setup() {
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(
      Optional.of(defaultPrison)
    )
    whenever(programServiceRepository.findByProgramCode(programCode)).thenReturn(
      defaultProgramService
    )
  }

  @Nested
  internal inner class CreateActivity {

    private val createRequest = CreateActivityRequest(
      prisonId = prisonId,
      code = "CA",
      programCode = programCode,
      description = "test description",
      capacity = 23,
      startDate = LocalDate.parse("2022-10-31"),
      endDate = LocalDate.parse("2022-11-30"),
      minimumIncentiveLevelCode = iepLevel,
      internalLocationId = roomId,
      payRates = listOf(
        PayRateRequest(
          incentiveLevel = "BASIC",
          payBand = "5",
          rate = BigDecimal(3.2),
        )
      )
    )

    @BeforeEach
    fun setup() {

      whenever(agencyInternalLocationRepository.findById(roomId)).thenReturn(Optional.of(defaultRoom))
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
        val prison = (it.arguments[0] as AgencyLocation)
        val code = (it.arguments[1] as String)
        return@thenAnswer AvailablePrisonIepLevel(code, prison, defaultIepLevel(code))
      }
      whenever(activityRepository.save(any())).thenAnswer {
        returnedCourseActivity = (it.arguments[0] as CourseActivity).copy(courseActivityId = 1)
        returnedCourseActivity
      }
      whenever(payBandRepository.findById(PayBand.pk(payBandCode))).thenReturn(Optional.of(defaultPayBand(payBandCode)))
    }

    private var returnedCourseActivity: CourseActivity? = null

    @Test
    fun `Activity data is mapped correctly`() {
      assertThat(activitiesService.createActivity(createRequest))
        .isEqualTo(CreateActivityResponse(courseActivityId))

      verify(activityRepository).save(
        check { activity ->
          assertThat(activity.code).isEqualTo("CA")
          assertThat(activity.prison.description).isEqualTo(prisonDescription)
          assertThat(activity.caseloadId).isEqualTo(prisonId)
          assertThat(activity.description).isEqualTo("test description")
          assertThat(activity.capacity).isEqualTo(23)
          assertThat(activity.active).isTrue
          assertThat(activity.program.programCode).isEqualTo(programCode)
          assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
          assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
          assertThat(activity.providerPartyCode).isEqualTo(prisonId)
          assertThat(activity.courseActivityType).isEqualTo("PA")
          assertThat(activity.iepLevel.code).isEqualTo(iepLevel)
          assertThat(activity.internalLocation.locationId).isEqualTo(roomId)
          assertThat(activity.payPerSession).isEqualTo(PayPerSession.H)
        }
      )

      val rate = returnedCourseActivity?.payRates?.first()
      assertThat(rate?.id?.iepLevelCode).isEqualTo("BASIC")
      assertThat(rate?.id?.payBandCode).isEqualTo("5")
      assertThat(rate?.id?.startDate).isEqualTo(LocalDate.parse("2022-10-31"))
      assertThat(rate?.endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      assertThat(rate?.halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal("0.001")))
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }

    @Test
    fun invalidRoom() {
      whenever(agencyInternalLocationRepository.findById(any())).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Location with id=$roomId does not exist")
    }

    @Test
    fun invalidProgramService() {
      whenever(programServiceRepository.findByProgramCode(any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Program Service with code=$programCode does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison $prisonId")
    }

    @Test
    fun invalidPayBandCode() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison $prisonId")
    }
  }

  @Nested
  internal inner class CreateOffenderProgramProfile {
    private val defaultOffender = Offender(
      nomsId = offenderNo,
      lastName = "Smith",
      gender = Gender("MALE", "Male")
    )
    private val defaultOffenderBooking = OffenderBooking(
      bookingId = offenderBookingId,
      offender = defaultOffender,
      location = defaultPrison,
      bookingBeginDate = LocalDateTime.now()
    )
    private val defaultCourseActivity = CourseActivity(
      courseActivityId = courseActivityId,
      prison = defaultPrison,
      program = defaultProgramService,
      iepLevel = defaultIepLevel("STD"),
      internalLocation = defaultRoom,
    ).apply {
      payRates.add(
        CourseActivityPayRate(
          id = CourseActivityPayRateId(
            courseActivity = this,
            startDate = LocalDate.parse("2022-11-01"),
            payBandCode = payBandCode,
            iepLevelCode = "ENH"
          ),
          payBand = defaultPayBand(payBandCode),
          endDate = LocalDate.parse("2022-11-03"),
          halfDayRate = BigDecimal("0.50"),
        )
      )
    }
    private val createRequest = CreateOffenderProgramProfileRequest(
      bookingId = offenderBookingId,
      startDate = LocalDate.parse("2022-10-31"),
      endDate = LocalDate.parse("2022-11-30"),
      payBandCode = payBandCode,
    )

    @BeforeEach
    fun setup() {
      whenever(activityRepository.findById(courseActivityId)).thenReturn(
        Optional.of(defaultCourseActivity)
      )
      whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(
        Optional.of(defaultOffenderBooking)
      )

      whenever(offenderProgramProfileRepository.save(any())).thenAnswer {
        (it.arguments[0] as OffenderProgramProfile).copy(offenderProgramReferenceId = offenderProgramReferenceId)
      }

      whenever(payBandRepository.findById(PayBand.pk(payBandCode))).thenReturn(Optional.of(defaultPayBand(payBandCode)))
    }

    @Test
    fun `Data is mapped correctly`() {
      assertThat(activitiesService.createOffenderProgramProfile(courseActivityId, createRequest))
        .isEqualTo(OffenderProgramProfileResponse(offenderProgramReferenceId))

      verify(offenderProgramProfileRepository).save(
        check {
          with(it) outer@{
            assertThat(offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
            assertThat(offenderBooking.bookingId).isEqualTo(offenderBookingId)
            assertThat(program.programCode).isEqualTo(programCode)
            assertThat(startDate).isEqualTo(LocalDate.parse("2022-10-31"))
            assertThat(programStatus).isEqualTo("ALLOC")
            assertThat(courseActivity?.courseActivityId).isEqualTo(courseActivityId)
            assertThat(prison?.id).isEqualTo(prisonId)
            assertThat(endDate).isEqualTo(LocalDate.parse("2022-11-30"))
            assertThat(payBands).hasSize(1)
            with(payBands.first()) {
              assertThat(offenderProgramProfile).isSameAs(this@outer)
              assertThat(startDate).isSameAs(this@outer.startDate)
              assertThat(endDate).isSameAs(this@outer.endDate)
              assertThat(payBand.code).isEqualTo(payBandCode)
            }
          }
        }
      )
    }

    @Test
    fun courseActivityNotFound() {
      whenever(activityRepository.findById(courseActivityId)).thenReturn(Optional.empty())

      val thrown = assertThrows<NotFoundException>() {
        activitiesService.createOffenderProgramProfile(courseActivityId, createRequest)
      }
      assertThat(thrown.message).isEqualTo("Course activity with id=$courseActivityId does not exist")
    }

    @Test
    fun invalidBooking() {
      whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createOffenderProgramProfile(courseActivityId, createRequest)
      }
      assertThat(thrown.message).isEqualTo("Booking with id=$offenderBookingId does not exist")
    }

    @Test
    fun invalidPayBandCode() {
      val thrown = assertThrows<BadDataException>() {
        activitiesService.createOffenderProgramProfile(courseActivityId, createRequest.copy(payBandCode = "doesnotexist"))
      }
      assertThat(thrown.message).isEqualTo("Pay band code doesnotexist does not exist")
    }

    @Test
    fun invalidPayBandCodeForCourse() {
      // We are testing that the pay band is not available for the course, not that the pay band does not exist at all
      whenever(payBandRepository.findById(PayBand.pk("not_on_course"))).thenReturn(Optional.of(defaultPayBand("not_on_course")))

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createOffenderProgramProfile(courseActivityId, createRequest.copy(payBandCode = "not_on_course"))
      }
      assertThat(thrown.message).isEqualTo("Pay band code not_on_course does not exist for course activity with id=$courseActivityId")
    }
  }
}

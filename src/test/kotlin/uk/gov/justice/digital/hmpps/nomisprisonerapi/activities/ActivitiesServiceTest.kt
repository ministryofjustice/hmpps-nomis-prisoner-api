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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
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

internal class ActivitiesServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val programServiceRepository: ProgramServiceRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val activitiesService = ActivitiesService(
    activityRepository,
    agencyLocationRepository,
    agencyInternalLocationRepository,
    programServiceRepository,
    availablePrisonIepLevelRepository,
    offenderBookingRepository,
    offenderProgramProfileRepository,
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
    }

    private var returnedCourseActivity: CourseActivity? = null

    @Test
    fun `Activity data is mapped correctly`() {
      Assertions.assertThat(activitiesService.createActivity(createRequest))
        .isEqualTo(CreateActivityResponse(courseActivityId))

      verify(activityRepository).save(
        org.mockito.kotlin.check { activity ->
          Assertions.assertThat(activity.code).isEqualTo("CA")
          Assertions.assertThat(activity.prison.description).isEqualTo(prisonDescription)
          Assertions.assertThat(activity.caseloadId).isEqualTo(prisonId)
          Assertions.assertThat(activity.description).isEqualTo("test description")
          Assertions.assertThat(activity.capacity).isEqualTo(23)
          Assertions.assertThat(activity.active).isTrue()
          Assertions.assertThat(activity.program.programCode).isEqualTo(programCode)
          Assertions.assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
          Assertions.assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
          Assertions.assertThat(activity.providerPartyCode).isEqualTo(prisonId)
          Assertions.assertThat(activity.courseActivityType).isEqualTo("PA")
          Assertions.assertThat(activity.iepLevel.code).isEqualTo(iepLevel)
          Assertions.assertThat(activity.internalLocation.locationId).isEqualTo(roomId)
          Assertions.assertThat(activity.payPerSession).isEqualTo(PayPerSession.H)
        }
      )

      val rate = returnedCourseActivity?.payRates?.first()
      Assertions.assertThat(rate?.iepLevelCode).isEqualTo("BASIC")
      Assertions.assertThat(rate?.payBandCode).isEqualTo("5")
      Assertions.assertThat(rate?.startDate).isEqualTo(LocalDate.parse("2022-10-31"))
      Assertions.assertThat(rate?.endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      Assertions.assertThat(rate?.halfDayRate).isEqualTo(BigDecimal(3.2))
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }

    @Test
    fun invalidRoom() {
      whenever(agencyInternalLocationRepository.findById(any())).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Location with id=$roomId does not exist")
    }

    @Test
    fun invalidProgramService() {
      whenever(programServiceRepository.findByProgramCode(any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Program Service with code=$programCode does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison $prisonId")
    }
  }

  @Nested
  internal inner class CreateOffenderProgramProfile {
    private val defaultOffender = Offender(
      nomsId = offenderNo, lastName = "Smith",
      gender = Gender("MALE", "Male")
    )
    private val defaultOffenderBooking = OffenderBooking(
      bookingId = offenderBookingId,
      offender = defaultOffender,
      bookingBeginDate = LocalDateTime.now()
    )
    private val defaultCourseActivity = CourseActivity(
      courseActivityId = courseActivityId,
      prison = defaultPrison,
      program = defaultProgramService,
      iepLevel = defaultIepLevel("STD"),
      internalLocation = defaultRoom,
    )
    private val createRequest = CreateOffenderProgramProfileRequest(
      bookingId = offenderBookingId,
      startDate = LocalDate.parse("2022-10-31"),
      endDate = LocalDate.parse("2022-11-30"),
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
    }

    @Test
    fun `Data is mapped correctly`() {
      Assertions.assertThat(activitiesService.createOffenderProgramProfile(courseActivityId, createRequest))
        .isEqualTo(CreateOffenderProgramProfileResponse(offenderProgramReferenceId))

      verify(offenderProgramProfileRepository).save(
        org.mockito.kotlin.check {
          with(it) {
            Assertions.assertThat(offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
            Assertions.assertThat(offenderBooking.bookingId).isEqualTo(offenderBookingId)
            Assertions.assertThat(program.programCode).isEqualTo(programCode)
            Assertions.assertThat(startDate).isEqualTo(LocalDate.parse("2022-10-31"))
            Assertions.assertThat(programStatus).isEqualTo("ALLOC")
            Assertions.assertThat(courseActivity?.courseActivityId).isEqualTo(courseActivityId)
            Assertions.assertThat(prison?.id).isEqualTo(prisonId)
            Assertions.assertThat(endDate).isEqualTo(LocalDate.parse("2022-11-30"))
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
      Assertions.assertThat(thrown.message).isEqualTo("Course activity with id=$courseActivityId does not exist")
    }

    @Test
    fun invalidBooking() {
      whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        activitiesService.createOffenderProgramProfile(courseActivityId, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Booking with id=$offenderBookingId does not exist")
    }
  }
}

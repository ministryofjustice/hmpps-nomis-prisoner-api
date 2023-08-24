package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.ScheduleRuleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

private const val COURSE_ACTIVITY_ID = 1L
private const val PRISON_ID = "BXI"
private const val ROOM_ID: Long = -3005
private const val PROGRAM_CODE = "TEST"
private const val IEP_LEVEL = "STD"
private const val PRISON_DESCRIPTION = "Brixton"

class ActivityServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val programServiceRepository: ProgramServiceRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val payRatesService: PayRatesService = mock()
  private val scheduleService: ScheduleService = mock()
  private val scheduleRuleService: ScheduleRuleService = mock()
  private val courseActivityRepository: CourseActivityRepository = mock()
  private val courseAllocationService: AllocationService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val activityService = ActivityService(
    activityRepository,
    agencyLocationRepository,
    agencyInternalLocationRepository,
    programServiceRepository,
    availablePrisonIepLevelRepository,
    payRatesService,
    scheduleService,
    scheduleRuleService,
    courseActivityRepository,
    courseAllocationService,
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

  @Nested
  internal inner class CreateActivity {

    private val createRequest = CreateActivityRequest(
      prisonId = PRISON_ID,
      code = "CA",
      programCode = PROGRAM_CODE,
      description = "test description",
      capacity = 23,
      startDate = LocalDate.parse("2022-10-31"),
      endDate = LocalDate.parse("2022-11-30"),
      minimumIncentiveLevelCode = IEP_LEVEL,
      internalLocationId = ROOM_ID,
      payRates = listOf(
        PayRateRequest(
          incentiveLevel = "BAS",
          payBand = "5",
          rate = BigDecimal(3.2),
        ),
      ),
      payPerSession = PayPerSession.H,
      excludeBankHolidays = true,
      outsideWork = true,
    )

    @BeforeEach
    fun setup() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(
        Optional.of(defaultPrison),
      )
      whenever(programServiceRepository.findByProgramCode(PROGRAM_CODE)).thenReturn(
        defaultProgramService,
      )
      whenever(agencyInternalLocationRepository.findById(ROOM_ID)).thenReturn(Optional.of(defaultRoom))
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
      assertThat(activityService.createActivity(createRequest))
        .isEqualTo(CreateActivityResponse(COURSE_ACTIVITY_ID))

      verify(activityRepository).save(
        check { activity ->
          assertThat(activity.code).isEqualTo("CA")
          assertThat(activity.prison.description).isEqualTo(PRISON_DESCRIPTION)
          assertThat(activity.caseloadId).isEqualTo(PRISON_ID)
          assertThat(activity.description).isEqualTo("test description")
          assertThat(activity.capacity).isEqualTo(23)
          assertThat(activity.active).isTrue
          assertThat(activity.program.programCode).isEqualTo(PROGRAM_CODE)
          assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
          assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
          assertThat(activity.providerPartyCode).isEqualTo(PRISON_ID)
          assertThat(activity.courseActivityType).isEqualTo("PA")
          assertThat(activity.iepLevel.code).isEqualTo(IEP_LEVEL)
          assertThat(activity.internalLocation?.locationId).isEqualTo(ROOM_ID)
          assertThat(activity.payPerSession).isEqualTo(PayPerSession.H)
          assertThat(activity.excludeBankHolidays).isTrue()
          assertThat(activity.outsideWork).isTrue()
        },
      )
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$PRISON_ID does not exist")
    }

    @Test
    fun invalidRoom() {
      whenever(agencyInternalLocationRepository.findById(any())).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Location with id=$ROOM_ID does not exist")
    }

    @Test
    fun invalidProgramService() {
      whenever(programServiceRepository.findByProgramCode(any())).thenReturn(null)

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Program Service with code=$PROGRAM_CODE does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), eq(IEP_LEVEL))).thenReturn(null)

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison $PRISON_ID")
    }
  }

  @Nested
  inner class UpdateActivity {

    private lateinit var courseActivity: CourseActivity
    private var returnedCourseActivity: CourseActivity? = null
    private val updateRequest = UpdateActivityRequest(
      startDate = LocalDate.parse("2022-11-01"),
      endDate = LocalDate.parse("2022-11-02"),
      internalLocationId = ROOM_ID + 1,
      capacity = 24,
      payRates = listOf(
        PayRateRequest(
          incentiveLevel = "BAS",
          payBand = "6",
          rate = BigDecimal(3.3).setScale(3, RoundingMode.HALF_UP),
        ),
      ),
      description = "test course activity updated",
      minimumIncentiveLevelCode = "BAS",
      payPerSession = PayPerSession.F,
      scheduleRules = listOf(
        ScheduleRuleRequest(
          startTime = LocalTime.parse("10:30"),
          endTime = LocalTime.parse("13:30"),
          monday = false,
          tuesday = false,
          wednesday = false,
          thursday = false,
          friday = false,
          saturday = true,
          sunday = true,
        ),
      ),
      excludeBankHolidays = false,
      outsideWork = false,
      programCode = "INTTEST",
    )
    val nomisDataBuilder = NomisDataBuilder()

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity()
        }
      }
      whenever(activityRepository.findById(anyLong())).thenReturn(Optional.of(courseActivity))
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(
        Optional.of(defaultPrison),
      )
      whenever(agencyInternalLocationRepository.findById(ROOM_ID + 1)).thenReturn(Optional.of(defaultRoom.copy(locationId = ROOM_ID + 1)))
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
        val prison = (it.arguments[0] as AgencyLocation)
        val code = (it.arguments[1] as String)
        return@thenAnswer AvailablePrisonIepLevel(code, prison, defaultIepLevel(code))
      }
      whenever(activityRepository.saveAndFlush(any())).thenAnswer {
        returnedCourseActivity = (it.arguments[0] as CourseActivity).copy(courseActivityId = 1)
        returnedCourseActivity
      }
      whenever(programServiceRepository.findByProgramCode(anyString())).thenReturn(
        ProgramService(20, "INTTEST", "test", true),
      )
    }

    @Test
    fun `should throw if location not found`() {
      whenever(agencyInternalLocationRepository.findById(anyLong())).thenReturn(Optional.empty())

      assertThatThrownBy {
        activityService.updateActivity(courseActivity.courseActivityId, updateRequest)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Location with id=${ROOM_ID + 1} does not exist")

      verify(agencyInternalLocationRepository).findById(ROOM_ID + 1)
    }

    @Test
    fun `should throw if location in different prison`() {
      whenever(agencyInternalLocationRepository.findById(anyLong())).thenReturn(Optional.of(defaultRoom.copy(agencyId = "WRONG_PRISON")))

      assertThatThrownBy {
        activityService.updateActivity(courseActivity.courseActivityId, updateRequest)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Location with id=${ROOM_ID + 1} not found in prison ${courseActivity.caseloadId}")
    }

    @Test
    fun `should throw if iep level not available in prison`() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), anyString())).thenReturn(null)

      assertThatThrownBy {
        activityService.updateActivity(courseActivity.courseActivityId, updateRequest)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("IEP type BAS does not exist for prison ${courseActivity.caseloadId}")
    }

    @Test
    fun `should update OK`() {
      activityService.updateActivity(courseActivity.courseActivityId, updateRequest)

      verify(activityRepository).saveAndFlush(
        check { activity ->
          assertThat(activity.description).isEqualTo("test course activity updated")
          assertThat(activity.capacity).isEqualTo(24)
          assertThat(activity.active).isTrue
          assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-11-01"))
          assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-02"))
          assertThat(activity.iepLevel.code).isEqualTo("BAS")
          assertThat(activity.internalLocation?.locationId).isEqualTo(ROOM_ID + 1)
          assertThat(activity.payPerSession).isEqualTo(PayPerSession.F)
          assertThat(activity.excludeBankHolidays).isFalse()
          assertThat(activity.outsideWork).isFalse()
        },
      )
    }

    @Test
    fun `should update nullables OK`() {
      assertDoesNotThrow {
        activityService.updateActivity(courseActivity.courseActivityId, updateRequest.copy(endDate = null, internalLocationId = null))
      }

      verify(activityRepository).saveAndFlush(
        check { activity ->
          assertThat(activity.scheduleEndDate).isNull()
          assertThat(activity.internalLocation).isNull()
        },
      )
    }

    @Test
    fun `should capture telemetry`() {
      lateinit var newPayRate: CourseActivityPayRate
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity(courseActivityId = 1) {
            courseSchedule()
            courseScheduleRule()
            newPayRate = payRate(halfDayRate = 4.3)
          }
        }
      }
      whenever(activityRepository.findById(anyLong())).thenReturn(Optional.of(courseActivity))
      whenever(payRatesService.buildNewPayRates(anyList(), any())).thenReturn(mutableListOf(newPayRate))

      activityService.updateActivity(courseActivity.courseActivityId, updateRequest.copy(internalLocationId = null))

      verify(telemetryClient).trackEvent(
        eq("activity-updated"),
        check {
          assertThat(it).containsEntry("nomisCourseActivityId", courseActivity.courseActivityId.toString())
          assertThat(it).containsEntry("prisonId", courseActivity.prison.id)
        },
        isNull(),
      )
    }
  }
}

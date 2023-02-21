package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

private const val COURSE_ACTIVITY_ID = 1L
private const val PRISON_ID = "LEI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "TEST"
private const val IEP_LEVEL = "STD"
private const val PRISON_DESCRIPTION = "Leeds"

class ActivityServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val programServiceRepository: ProgramServiceRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val payRatesService: PayRatesService = mock()
  private val scheduleService: ScheduleService = mock()
  private val scheduleRuleService: ScheduleRuleService = mock()
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
    locationId = ROOM_ID
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
        )
      ),
      payPerSession = "H",
    )

    @BeforeEach
    fun setup() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(
        Optional.of(defaultPrison)
      )
      whenever(programServiceRepository.findByProgramCode(PROGRAM_CODE)).thenReturn(
        defaultProgramService
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
      Assertions.assertThat(activityService.createActivity(createRequest))
        .isEqualTo(CreateActivityResponse(COURSE_ACTIVITY_ID))

      verify(activityRepository).save(
        org.mockito.kotlin.check { activity ->
          Assertions.assertThat(activity.code).isEqualTo("CA")
          Assertions.assertThat(activity.prison.description).isEqualTo(PRISON_DESCRIPTION)
          Assertions.assertThat(activity.caseloadId).isEqualTo(PRISON_ID)
          Assertions.assertThat(activity.description).isEqualTo("test description")
          Assertions.assertThat(activity.capacity).isEqualTo(23)
          Assertions.assertThat(activity.active).isTrue
          Assertions.assertThat(activity.program.programCode).isEqualTo(PROGRAM_CODE)
          Assertions.assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
          Assertions.assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
          Assertions.assertThat(activity.providerPartyCode).isEqualTo(PRISON_ID)
          Assertions.assertThat(activity.courseActivityType).isEqualTo("PA")
          Assertions.assertThat(activity.iepLevel.code).isEqualTo(IEP_LEVEL)
          Assertions.assertThat(activity.internalLocation?.locationId).isEqualTo(ROOM_ID)
          Assertions.assertThat(activity.payPerSession).isEqualTo(PayPerSession.H)
        }
      )
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Prison with id=$PRISON_ID does not exist")
    }

    @Test
    fun invalidRoom() {
      whenever(agencyInternalLocationRepository.findById(any())).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Location with id=$ROOM_ID does not exist")
    }

    @Test
    fun invalidProgramService() {
      whenever(programServiceRepository.findByProgramCode(any())).thenReturn(null)

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Program Service with code=$PROGRAM_CODE does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), eq(IEP_LEVEL))).thenReturn(null)

      val thrown = assertThrows<BadDataException> {
        activityService.createActivity(createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison $PRISON_ID")
    }
  }
}

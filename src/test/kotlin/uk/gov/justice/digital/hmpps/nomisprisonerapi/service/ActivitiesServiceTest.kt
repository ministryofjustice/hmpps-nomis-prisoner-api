package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.PayRateRequest
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

private const val courseActivityId = 1L
private const val prisonId = "LEI"
private const val roomId: Long = -8
private const val programCode = "TEST"
private const val iepLevel = "STD"

internal class ActivitiesServiceTest {

  private val activityRepository: ActivityRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val programServiceRepository: ProgramServiceRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val activitiesService = ActivitiesService(
    activityRepository,
    agencyLocationRepository,
    agencyInternalLocationRepository,
    programServiceRepository,
    availablePrisonIepLevelRepository,
    telemetryClient,
  )

  private var returnedCourseActivity: CourseActivity? = null

  @BeforeEach
  fun setup() {
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(
      Optional.of(AgencyLocation(prisonId, "Leeds"))
    )
    whenever(agencyInternalLocationRepository.findById(roomId)).thenReturn(
      Optional.of(
        AgencyInternalLocation(
          agencyId = prisonId,
          description = "desc",
          locationType = "ROOM",
          locationCode = "ROOM-1",
          locationId = roomId
        )
      )
    )
    whenever(programServiceRepository.findByProgramCode(programCode)).thenReturn(
      ProgramService(
        programCode = programCode,
        description = "desc",
      )
    )
    whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
      val prison = (it.arguments[0] as AgencyLocation)
      val code = (it.arguments[1] as String)
      return@thenAnswer AvailablePrisonIepLevel(code, prison, IEPLevel(code, "$code-desc"))
    }
    whenever(activityRepository.save(any())).thenAnswer {
      returnedCourseActivity = (it.arguments[0] as CourseActivity).copy(courseActivityId = 1)
      returnedCourseActivity
    }
  }

  @DisplayName("create")
  @Nested
  internal inner class Create {
    private val createRequest = CreateActivityRequest(
      prisonId = prisonId,
      code = "CA",
      programCode = programCode,
      description = "test description",
      capacity = 23,
      startDate = LocalDate.parse("2022-10-31"),
      endDate = LocalDate.parse("2022-11-30"),
      minimumIncentiveLevel = iepLevel,
      internalLocation = roomId,
      payRates = listOf(
        PayRateRequest(
          incentiveLevel = "BASIC",
          payBand = "5",
          rate = BigDecimal(3.2),
        )
      )
    )

    @Test
    fun `Activity data is mapped correctly`() {
      assertThat(activitiesService.createActivity(createRequest))
        .isEqualTo(CreateActivityResponse(courseActivityId))

      verify(activityRepository).save(
        org.mockito.kotlin.check { activity ->
          assertThat(activity.code).isEqualTo("CA")
          assertThat(activity.prison.description).isEqualTo("Leeds")
          assertThat(activity.caseloadId).isEqualTo(prisonId)
          assertThat(activity.description).isEqualTo("test description")
          assertThat(activity.capacity).isEqualTo(23)
          assertThat(activity.active).isTrue()
          assertThat(activity.program?.programCode).isEqualTo(programCode)
          assertThat(activity.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
          assertThat(activity.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
          assertThat(activity.providerPartyCode).isEqualTo(prisonId)
          assertThat(activity.courseActivityType).isEqualTo("PA")
          assertThat(activity.iepLevel.code).isEqualTo(iepLevel)
          assertThat(activity.internalLocation?.locationId).isEqualTo(roomId)
          assertThat(activity.payPerSession).isEqualTo(PayPerSession.F)
        }
      )

      val rate = returnedCourseActivity?.payRates?.first()
      assertThat(rate?.iepLevelCode).isEqualTo("BASIC")
      assertThat(rate?.payBandCode).isEqualTo("5")
      assertThat(rate?.startDate).isEqualTo(LocalDate.parse("2022-10-31"))
      assertThat(rate?.endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      assertThat(rate?.halfDayRate).isEqualTo(BigDecimal(3.2))
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
  }
}

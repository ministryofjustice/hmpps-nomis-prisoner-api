package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityPayRateBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private const val PRISON_ID = "LEI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "TEST"
private const val IEP_LEVEL = "STD"
private const val PRISON_DESCRIPTION = "Leeds"
private const val PAY_BAND_CODE = "5"

class PayRateServiceTest {

  private val nomisDataBuilder = NomisDataBuilder()
  private lateinit var courseActivity: CourseActivity
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository = mock()
  private val payBandRepository: ReferenceCodeRepository<PayBand> = mock()
  private val payRatesService = PayRatesService(
    availablePrisonIepLevelRepository,
    offenderProgramProfileRepository,
    payBandRepository,
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

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)
  private val threeDaysAgo = today.minusDays(3)

  @Nested
  internal inner class CreatePayRates {

    private val courseActivity = CourseActivity(
      courseActivityId = 1L,
      code = "CA",
      caseloadId = PRISON_ID,
      prison = defaultPrison,
      program = defaultProgramService,
      scheduleStartDate = LocalDate.parse("2022-10-31"),
      iepLevel = defaultIepLevel("BAS"),
      internalLocation = defaultRoom,
    )

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
    )

    @BeforeEach
    fun `set up validation mocks`() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
        val prison = (it.arguments[0] as AgencyLocation)
        val code = (it.arguments[1] as String)
        AvailablePrisonIepLevel(code, prison, defaultIepLevel(code))
      }
      whenever(payBandRepository.findById(any())).thenAnswer {
        Optional.of(PayBand((it.arguments[0] as Pk).code, ""))
      }
    }

    @Test
    fun `Pay rates are mapped correctly`() {
      val payRates = payRatesService.mapRates(createRequest, courseActivity)

      val rate = payRates.first()
      assertThat(rate.id.iepLevelCode).isEqualTo("BAS")
      assertThat(rate.id.payBandCode).isEqualTo("5")
      assertThat(rate.id.startDate).isEqualTo(LocalDate.parse("2022-10-31"))
      assertThat(rate.endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      assertThat(rate.halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal("0.001")))
    }

    @Test
    fun invalidPayBand() {
      whenever(payBandRepository.findById(PayBand.pk(PAY_BAND_CODE))).thenReturn(Optional.empty())

      assertThatThrownBy {
        payRatesService.mapRates(createRequest, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Pay band code $PAY_BAND_CODE does not exist")
    }

    @Test
    fun invalidPayBandIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), eq("BAS"))).thenReturn(null)

      assertThatThrownBy {
        payRatesService.mapRates(createRequest, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Pay rate IEP type BAS does not exist for prison $PRISON_ID")
    }
  }

  @Nested
  inner class BuildNewPayRates {
    @BeforeEach
    fun `set up validation mocks`() {
      // The default existing activity has 1 active pay rate - iepLevel = "STD", payBand = "5", halfDayRate = 3.2
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity()
        }
      }
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
        val prison = (it.arguments[0] as AgencyLocation)
        val code = (it.arguments[1] as String)
        AvailablePrisonIepLevel(code, prison, defaultIepLevel(code))
      }
      whenever(payBandRepository.findById(any())).thenAnswer {
        Optional.of(PayBand((it.arguments[0] as Pk).code, ""))
      }
    }

    @Test
    fun `no change should do nothing`() {
      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "5", rate = BigDecimal(3.2)))
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(1)
      with(newPayRates.first()) {
        assertThat(this.iepLevel.code).isEqualTo("STD")
        assertThat(this.payBand.code).isEqualTo("5")
        assertThat(this.halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal("0.001")))
      }
    }

    @Test
    fun `adding should create new pay rate effective today`() {
      val request = listOf(
        PayRateRequest(incentiveLevel = "STD", payBand = "5", rate = BigDecimal(3.2)),
        PayRateRequest(incentiveLevel = "STD", payBand = "6", rate = BigDecimal(3.4)),
      )
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(2)
      // existing rate unchanged
      with(newPayRates.findRate("STD", "5")) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isNull()
      }
      // new rate added
      with(newPayRates.findRate("STD", "6")) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.4), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(today)
        assertThat(endDate).isNull()
      }
    }

    @Test
    fun `amending should expire existing and create new pay rate effective tomorrow`() {
      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "5", rate = BigDecimal(4.3)))
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(2)
      // old rate has been expired
      with(newPayRates.findRate("STD", "5", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
      // new rate created from tomorrow
      with(newPayRates.findRate("STD", "5", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(tomorrow)
        assertThat(endDate).isNull()
      }
    }

    @Test
    fun `re-adding previously expired rate should add new rate effective today`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseSchedule()
            courseScheduleRule()
            payRate(endDate = threeDaysAgo.toString())
          }
        }
      }

      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "5", rate = BigDecimal(4.3)))
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(2)
      // old rate not changed
      with(newPayRates.findRate("STD", "5", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(threeDaysAgo)
      }
      // new rate created from today
      with(newPayRates.findRate("STD", "5", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(today)
        assertThat(endDate).isNull()
      }
    }

    @Test
    fun `amending new rate starting tomorrow should not cause an expiry (adjusting rate twice in one day)`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseSchedule()
            courseScheduleRule()
            payRate(endDate = today.toString())
            payRate(startDate = tomorrow.toString(), halfDayRate = 4.3)
          }
        }
      }
      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "5", rate = BigDecimal(5.4)))
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(2)
      // old rate still expired
      with(newPayRates.findRate("STD", "5", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
      // new rate with adjusted half day rate is still effective from tomorrow
      with(newPayRates.findRate("STD", "5", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(tomorrow)
        assertThat(endDate).isNull()
      }
    }

    @Test
    fun `removing new rate starting tomorrow should delete the rate rather than expire`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseSchedule()
            courseScheduleRule()
            payRate(endDate = today.toString())
            payRate(startDate = tomorrow.toString(), halfDayRate = 4.3)
          }
        }
      }

      val newPayRates = payRatesService.buildNewPayRates(listOf(), courseActivity)

      // We only have the old expired rate - the future rate is now removed
      assertThat(newPayRates.size).isEqualTo(1)
      // old rate still expired
      with(newPayRates.first()) {
        assertThat(id.payBandCode).isEqualTo("5")
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
    }

    @Test
    fun `missing rate should be expired`() {
      // request pay band 6 instead of 5
      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "6", rate = BigDecimal(4.3)))
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(2)
      // missing rate for pay band 5 has been expired
      with(newPayRates.findRate("STD", "5", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
      // new rate for pay band 6 effective from today
      with(newPayRates.findRate("STD", "6", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(today)
        assertThat(endDate).isNull()
      }
    }

    @Test
    fun `should be able to add, amend, amend future rate and delete rate at the same time`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseSchedule()
            courseScheduleRule()
            payRate(endDate = yesterday.toString())
            payRate(startDate = tomorrow.toString(), halfDayRate = 4.3)
            payRate(payBandCode = "6", endDate = today.toString(), halfDayRate = 5.3)
            payRate(payBandCode = "7", halfDayRate = 8.7)
          }
        }
      }
      val request = listOf(
        PayRateRequest("STD", "5", BigDecimal(4.4)),
        PayRateRequest("STD", "6", BigDecimal(5.4)),
      )
      val newPayRates = payRatesService.buildNewPayRates(request, courseActivity)

      assertThat(newPayRates.size).isEqualTo(5)
      // old rate for pay band 5 is still expired
      with(newPayRates.findRate("STD", "5", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(yesterday)
      }
      // new rate for pay band 5 has been updated
      with(newPayRates.findRate("STD", "5", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(4.4), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(tomorrow)
      }
      // old rate for pay band 6 is still expired
      with(newPayRates.findRate("STD", "6", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(5.3), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
      // new rate for pay band 6 applicable from tomorrow
      with(newPayRates.findRate("STD", "6", expired = false)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
        assertThat(id.startDate).isEqualTo(tomorrow)
      }
      // old rate for pay band 7 has been expired
      with(newPayRates.findRate("STD", "7", expired = true)) {
        assertThat(halfDayRate).isCloseTo(BigDecimal(8.7), within(BigDecimal(0.001)))
        assertThat(endDate).isEqualTo(today)
      }
    }

    @Test
    fun invalidPayBandIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), eq("BAS"))).thenReturn(null)
      val request = listOf(PayRateRequest(incentiveLevel = "BAS", payBand = "5", rate = BigDecimal(3.2)))

      assertThatThrownBy {
        payRatesService.buildNewPayRates(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Pay rate IEP type BAS does not exist for prison $PRISON_ID")
    }

    @Test
    fun invalidPayBand() {
      whenever(payBandRepository.findById(PayBand.pk("A"))).thenReturn(Optional.empty())
      val request = listOf(PayRateRequest(incentiveLevel = "STD", payBand = "A", rate = BigDecimal(3.2)))

      assertThatThrownBy {
        payRatesService.buildNewPayRates(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Pay band code A does not exist")
    }

    private fun MutableList<CourseActivityPayRate>.findRate(
      iepLevelCode: String,
      payBandCode: String,
      expired: Boolean = false,
    ): CourseActivityPayRate =
      firstOrNull { it.id.iepLevelCode == iepLevelCode && it.id.payBandCode == payBandCode && if (expired) it.endDate != null else it.endDate == null }!!
  }

  @Nested
  inner class BuildUpdateTelemetry {

    private lateinit var oldRates: List<CourseActivityPayRate>

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity {
            oldRates = listOf(
              payRate(endDate = yesterday.toString()),
              payRate(startDate = tomorrow.toString(), halfDayRate = 4.3),
              payRate(payBandCode = "7", halfDayRate = 8.7),
            )
          }
        }
      }
    }

    @Test
    fun `should publish expiry, creation and update of pay rates`() {
      val newRates = mutableListOf<CourseActivityPayRate>()
      nomisDataBuilder.build {
        programService {
          courseActivity {
            newRates.addAll(
              listOf(
                payRate(endDate = yesterday.toString()),
                payRate(startDate = tomorrow.toString(), halfDayRate = 4.4),
                payRate(startDate = tomorrow.toString(), payBandCode = "6", halfDayRate = 5.4),
                payRate(endDate = today.toString(), payBandCode = "7", halfDayRate = 8.7),
              ),
            )
          }
        }
      }

      val telemetry = payRatesService.buildUpdateTelemetry(oldRates, newRates)

      assertThat(telemetry["created-courseActivityPayRateIds"]).isEqualTo("[STD-6-$tomorrow]")
      assertThat(telemetry["updated-courseActivityPayRateIds"]).isEqualTo("[STD-5-$tomorrow]")
      assertThat(telemetry["expired-courseActivityPayRateIds"]).isEqualTo("[STD-7-2022-10-31]")
    }

    @Test
    fun `should not publish telemetry for no change`() {
      val newRates = mutableListOf<CourseActivityPayRate>()
      nomisDataBuilder.build {
        programService {
          courseActivity {
            newRates.addAll(
              listOf(
                payRate(endDate = yesterday.toString()),
                payRate(startDate = tomorrow.toString(), halfDayRate = 4.3),
                payRate(payBandCode = "7", halfDayRate = 8.7),
              ),
            )
          }
        }
      }

      val telemetry = payRatesService.buildUpdateTelemetry(oldRates, newRates)

      assertThat(telemetry["created-courseActivityPayRateIds"]).isNull()
      assertThat(telemetry["updated-courseActivityPayRateIds"]).isNull()
      assertThat(telemetry["expired-courseActivityPayRateIds"]).isNull()
    }
  }

  private fun rateFactory() = CourseActivityPayRateBuilderFactory()
  private fun activityFactory() = CourseActivityBuilderFactory()
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

private const val PRISON_ID = "LEI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "TEST"
private const val IEP_LEVEL = "STD"
private const val PRISON_DESCRIPTION = "Leeds"
private const val PAY_BAND_CODE = "5"

class PayRateServiceTest {

  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository = mock()
  private val payBandRepository: ReferenceCodeRepository<PayBand> = mock()
  private val payRatesService = PayRatesService(availablePrisonIepLevelRepository, offenderProgramProfileRepository, payBandRepository)

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

  private fun defaultPayBand(code: String) = PayBand(code, "Pay band $code")

  @Nested
  internal inner class CreatePayRates {

    private val courseActivity = CourseActivity(
      courseActivityId = 1L, code = "CA", caseloadId = PRISON_ID, prison = defaultPrison,
      program = defaultProgramService, iepLevel = defaultIepLevel("BAS"), internalLocation = defaultRoom
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
        )
      ),
      payPerSession = "H",
    )
    @Test
    fun `Pay rates are mapped correctly`() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
        val prison = (it.arguments[0] as AgencyLocation)
        val code = (it.arguments[1] as String)
        return@thenAnswer AvailablePrisonIepLevel(code, prison, defaultIepLevel(code))
      }
      whenever(payBandRepository.findById(PayBand.pk(PAY_BAND_CODE))).thenReturn(Optional.of(defaultPayBand(PAY_BAND_CODE)))

      val payRates = payRatesService.mapRates(createRequest, courseActivity)

      val rate = payRates.first()
      Assertions.assertThat(rate.id.iepLevelCode).isEqualTo("BAS")
      Assertions.assertThat(rate.id.payBandCode).isEqualTo("5")
      Assertions.assertThat(rate.id.startDate).isEqualTo(LocalDate.parse("2022-10-31"))
      Assertions.assertThat(rate.endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      Assertions.assertThat(rate.halfDayRate).isCloseTo(BigDecimal(3.2), Assertions.within(BigDecimal("0.001")))
    }
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

private const val offenderBookingId = -9L
private const val offenderNo = "A1234AA"
private const val prisonId = "SWI"
private const val prisonDescription = "Shrewsbury"

internal class IncentiveServiceTest {

  private val incentiveRepository: IncentiveRepository = mock()
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val incentivesService = IncentivesService(
    incentiveRepository,
    offenderBookingRepository,
    agencyLocationRepository,
    availablePrisonIepLevelRepository,
    telemetryClient,
  )

  private val defaultOffender = Offender(
    nomsId = offenderNo, lastName = "Smith",
    gender = Gender("MALE", "Male")
  )
  private val defaultOffenderBooking = OffenderBooking(
    bookingId = offenderBookingId,
    offender = defaultOffender,
    bookingBeginDate = LocalDateTime.now()
  )

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(
      Optional.of(defaultOffenderBooking)
    )
    whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenAnswer {
      val prison = (it.arguments[0] as AgencyLocation)
      val code = (it.arguments[1] as String)
      return@thenAnswer AvailablePrisonIepLevel(code, prison, IEPLevel(code, "$code-desc"))
    }
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(
      Optional.of(AgencyLocation(prisonId, "desc"))
    )
    whenever(incentiveRepository.save(any())).thenAnswer {
      (it.arguments[0] as Incentive).copy(id = IncentiveId(defaultOffenderBooking, 1))
    }
  }

  @DisplayName("create")
  @Nested
  internal inner class CreateIncentive {
    private val createRequest = CreateIncentiveRequest(
      iepLevel = "STD",
      comments = "a comment",
      iepDateTime = LocalDateTime.parse("2021-12-01T13:04"),
      prisonId = prisonId,
      userId = "me",
    )

    @Test
    fun `incentive data is mapped correctly`() {
      Assertions.assertThat(incentivesService.createIncentive(offenderBookingId, createRequest))
        .isEqualTo(CreateIncentiveResponse(offenderBookingId, 1))

      val incentive = defaultOffenderBooking.incentives.get(0)

      Assertions.assertThat(incentive.commentText).isEqualTo("a comment")
      Assertions.assertThat(incentive.iepDate).isEqualTo(LocalDate.parse("2021-12-01"))
      Assertions.assertThat(incentive.iepTime).isEqualTo(LocalDateTime.parse("2021-12-01T13:04"))
      Assertions.assertThat(incentive.id.offenderBooking.bookingId).isEqualTo(offenderBookingId)
      Assertions.assertThat(incentive.iepLevel.description).isEqualTo("STD-desc")
      Assertions.assertThat(incentive.location).isEqualTo(AgencyLocation(prisonId, prisonDescription))
      Assertions.assertThat(incentive.userId).isEqualTo("me")
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(
        Optional.empty()
      )

      val thrown = assertThrows<NotFoundException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo(offenderBookingId.toString())
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(any(), any())).thenReturn(null)

      val thrown = assertThrows<BadDataException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      Assertions.assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison SWI")
    }
  }
}

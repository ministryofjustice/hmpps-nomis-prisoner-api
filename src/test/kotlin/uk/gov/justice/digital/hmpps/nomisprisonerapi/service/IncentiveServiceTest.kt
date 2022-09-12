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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateIncentiveRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateIncentiveResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

private const val offenderBookingId = -9L
private const val offenderNo = "A1234AA"
private const val prisonId = "SWI"
private const val prisonDescription = "Shrewsbury"

internal class IncentiveServiceTest {

  private val incentiveRepository: IncentiveRepository = mock()
  private val iepLevelRepository: ReferenceCodeRepository<IEPLevel> = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val incentivesService = IncentivesService(
    incentiveRepository,
    offenderBookingRepository,
    agencyLocationRepository,
    iepLevelRepository,
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
    whenever(iepLevelRepository.findById(any())).thenAnswer {
      val code = (it.arguments[0] as ReferenceCode.Pk).code!!
      return@thenAnswer Optional.of(IEPLevel(code, "$code-desc"))
    }
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(
      Optional.of(AgencyLocation(prisonId, "desc"))
    )

    // whenever(internalLocationRepository.save(any())).thenAnswer { it.arguments[0] as AgencyInternalLocation }
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
      iepDate = LocalDate.parse("2021-12-01"),
      iepTime = LocalTime.parse("13:04"),
      agencyId = prisonId,
      userId = "me",
    )

    @Test
    fun `visit data is mapped correctly`() {
      assertThat(incentivesService.createIncentive(offenderBookingId, createRequest))
        .isEqualTo(CreateIncentiveResponse(offenderBookingId, 1))

      verify(incentiveRepository).save(
        org.mockito.kotlin.check { incentive ->
          assertThat(incentive?.commentText).isEqualTo("a comment")
          assertThat(incentive?.iepDate).isEqualTo(LocalDate.parse("2021-12-01"))
          assertThat(incentive?.iepTime).isEqualTo(LocalTime.parse("13:04"))
          assertThat(incentive?.id?.offenderBooking?.bookingId).isEqualTo(offenderBookingId)
          assertThat(incentive?.iepLevel?.description).isEqualTo("STD-desc")
          assertThat(incentive?.location).isEqualTo(AgencyLocation(prisonId, prisonDescription))
          assertThat(incentive?.userId).isEqualTo("me")
        }
      )
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findById(offenderBookingId)).thenReturn(
        Optional.empty()
      )

      val thrown = assertThrows<NotFoundException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      assertThat(thrown.message).isEqualTo(offenderBookingId.toString())
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(iepLevelRepository.findById(any())).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException>() {
        incentivesService.createIncentive(offenderBookingId, createRequest)
      }
      assertThat(thrown.message).isEqualTo("Invalid IEP type from incentives: STD")
    }
  }
}

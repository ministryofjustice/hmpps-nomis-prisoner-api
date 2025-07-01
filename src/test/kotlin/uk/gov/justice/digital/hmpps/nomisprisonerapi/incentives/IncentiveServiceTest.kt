package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIepLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevelId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitAllowanceLevelRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

private const val OFFENDER_BOOKING_ID = -9L
private const val OFFENDER_NO = "A1234AA"
private const val PRISON_ID = "SWI"
private const val PRISON_DESCRIPTION = "Shrewsbury"

internal class IncentiveServiceTest {

  private val incentiveRepository: IncentiveRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val incentivesCodeRepository: ReferenceCodeRepository<IEPLevel> = mock()
  private val prisonIncentiveLevelRepository: PrisonIepLevelRepository = mock()
  private val visitAllowanceLevelRepository: VisitAllowanceLevelRepository = mock()

  private val incentivesService = IncentivesService(
    incentiveRepository,
    offenderBookingRepository,
    agencyLocationRepository,
    incentivesCodeRepository,
    visitAllowanceLevelRepository,
    prisonIncentiveLevelRepository,
    telemetryClient,
  )

  private val defaultOffender = Offender(
    nomsId = OFFENDER_NO,
    lastName = "Smith",
    firstName = "John",
    gender = Gender("MALE", "Male"),
  )
  private val defaultOffenderBooking = OffenderBooking(
    bookingId = OFFENDER_BOOKING_ID,
    offender = defaultOffender,
    bookingBeginDate = LocalDateTime.now(),
    location = AgencyLocation(PRISON_ID, PRISON_DESCRIPTION),
  )

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findById(OFFENDER_BOOKING_ID)).thenReturn(
      Optional.of(defaultOffenderBooking),
    )
    whenever(prisonIncentiveLevelRepository.findFirstByAgencyLocationAndIepLevelCode(any(), any())).thenAnswer {
      val prison = (it.arguments[0] as AgencyLocation)
      val code = (it.arguments[1] as String)
      return@thenAnswer PrisonIepLevel(code, prison, IEPLevel(code, "$code-desc"))
    }
    whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(
      Optional.of(AgencyLocation(PRISON_ID, "desc")),
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
      prisonId = PRISON_ID,
      userId = "me",
    )

    @Test
    fun `incentive data is mapped correctly`() {
      assertThat(incentivesService.createIncentive(OFFENDER_BOOKING_ID, createRequest))
        .isEqualTo(CreateIncentiveResponse(OFFENDER_BOOKING_ID, 1))

      val incentive = defaultOffenderBooking.incentives[0]

      assertThat(incentive.commentText).isEqualTo("a comment")
      assertThat(incentive.iepDate).isEqualTo(LocalDate.parse("2021-12-01"))
      assertThat(incentive.iepTime).isEqualTo(LocalDateTime.parse("2021-12-01T13:04"))
      assertThat(incentive.id.offenderBooking.bookingId).isEqualTo(OFFENDER_BOOKING_ID)
      assertThat(incentive.iepLevel.description).isEqualTo("STD-desc")
      assertThat(incentive.location).isEqualTo(AgencyLocation(PRISON_ID, PRISON_DESCRIPTION))
      assertThat(incentive.userId).isEqualTo("me")
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findById(OFFENDER_BOOKING_ID)).thenReturn(
        Optional.empty(),
      )

      val thrown = assertThrows<NotFoundException> {
        incentivesService.createIncentive(OFFENDER_BOOKING_ID, createRequest)
      }
      assertThat(thrown.message).isEqualTo(OFFENDER_BOOKING_ID.toString())
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        incentivesService.createIncentive(OFFENDER_BOOKING_ID, createRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$PRISON_ID does not exist")
    }

    @Test
    fun invalidIEP() {
      whenever(prisonIncentiveLevelRepository.findFirstByAgencyLocationAndIepLevelCode(any(), any())).thenReturn(null)

      val thrown = assertThrows<BadDataException> {
        incentivesService.createIncentive(OFFENDER_BOOKING_ID, createRequest)
      }
      assertThat(thrown.message).isEqualTo("IEP type STD does not exist for prison SWI")
    }
  }

  @DisplayName("create global incentive level")
  @Nested
  internal inner class CreateGlobalIncentiveLevel {

    @Test
    fun `sequence and parent code are set to max(sequence) + 1`() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("BIG"))).thenReturn(
        Optional.empty(),
      )
      whenever(incentivesCodeRepository.findAllByDomainOrderBySequenceAsc(IEPLevel.IEP_LEVEL)).thenReturn(
        listOf(IEPLevel("AAA", "desc", true, 1)),
      )
      whenever(incentivesCodeRepository.save(any())).thenReturn(IEPLevel("BIG", "desc"))
      incentivesService.createGlobalIncentiveLevel(CreateGlobalIncentiveRequest("BIG", "desc", true))

      verify(incentivesCodeRepository).save(
        org.mockito.kotlin.check { iep ->
          assertThat(iep.sequence).isEqualTo(2)
          assertThat(iep.parentCode).isEqualTo("2")
        },
      )
    }

    @Test
    fun `sequence and parent code are are set to 1 if no other IEP levels found`() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("BIG"))).thenReturn(
        Optional.empty(),
      )
      whenever(incentivesCodeRepository.findAllByDomainOrderBySequenceAsc(IEPLevel.IEP_LEVEL)).thenReturn(
        emptyList(),
      )
      whenever(incentivesCodeRepository.save(any())).thenReturn(IEPLevel("BIG", "desc"))

      incentivesService.createGlobalIncentiveLevel(CreateGlobalIncentiveRequest("BIG", "desc", true))
      verify(incentivesCodeRepository).save(
        org.mockito.kotlin.check { iep ->
          assertThat(iep.sequence).isEqualTo(1)
          assertThat(iep.parentCode).isEqualTo("1")
        },
      )
    }

    @Test
    fun `Attempt to create an existing global IEP level is ignored`() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("BIG"))).thenReturn(
        Optional.of(IEPLevel("AAA", "desc", true, 1)),
      )
      incentivesService.createGlobalIncentiveLevel(CreateGlobalIncentiveRequest("BIG", "desc", true))
      verify(incentivesCodeRepository, never()).save(any())
    }
  }

  @DisplayName("update global incentive level")
  @Nested
  internal inner class UpdateGlobalIncentiveLevel {
    private val existingActiveIepLevel = IEPLevel("ABC", "desc", true)
    private val existingInactiveIepLevel =
      IEPLevel(code = "ABC", description = "desc", active = false, expiredDate = LocalDate.of(2023, 1, 10))

    @Test
    fun `expiredDate is set to today when incentive level is made inactive`() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("ABC"))).thenReturn(
        Optional.of(existingActiveIepLevel),
      )
      assertThat(
        incentivesService.updateGlobalIncentiveLevel(
          "ABC",
          UpdateGlobalIncentiveRequest("desc", false),
        ).expiredDate,
      ).isToday
    }

    @Test
    fun `expiredDate is set to previous value when an inactive incentive level is updated `() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("ABC"))).thenReturn(
        Optional.of(existingInactiveIepLevel),
      )
      assertThat(
        incentivesService.updateGlobalIncentiveLevel(
          "ABC",
          UpdateGlobalIncentiveRequest("desc", false),
        ).expiredDate,
      ).isEqualTo(LocalDate.of(2023, 1, 10))
    }

    @Test
    fun `expiredDate is set to null for update to active incentive level`() {
      whenever(incentivesCodeRepository.findById(IEPLevel.pk("ABC"))).thenReturn(
        Optional.of(existingInactiveIepLevel),
      )
      assertThat(
        incentivesService.updateGlobalIncentiveLevel(
          "ABC",
          UpdateGlobalIncentiveRequest("desc", true),
        ).expiredDate,
      ).isNull()
    }
  }

  @DisplayName("create prison incentive level data")
  @Nested
  internal inner class CreatePrisonIncentiveLevelData {
    val prison = AgencyLocation("MDI", "desc")

    @Test
    fun `data is mapped correctly during creation`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(incentivesCodeRepository.findById(ReferenceCode.Pk("IEP_LEVEL", "NSTD"))).thenReturn(
        Optional.of(IEPLevel("NSTD", "desc", true, 1)),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.empty(),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.empty(),
      )
      whenever(prisonIncentiveLevelRepository.save(any())).thenReturn(getPrisonIncentiveLevel())
      whenever(visitAllowanceLevelRepository.save(any())).thenReturn(getVisitAllowanceLevel())
      incentivesService.createPrisonIncentiveLevelData(
        "MDI",
        CreatePrisonIncentiveRequest(
          levelCode = "NSTD",
          active = true,
          defaultOnAdmission = true,
          visitOrderAllowance = 3,
          privilegedVisitOrderAllowance = 4,
          remandTransferLimitInPence = 350,
          remandSpendLimitInPence = 3700,
          convictedTransferLimitInPence = 650,
          convictedSpendLimitInPence = 6600,
        ),
      )

      verify(prisonIncentiveLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(true)
          assertThat(data.default).isEqualTo(true)
          assertThat(data.expiryDate).isNull()
          assertThat(data.remandTransferLimit).isEqualTo(BigDecimal.valueOf(3.5))
          assertThat(data.remandSpendLimit).isEqualTo(BigDecimal.valueOf(37.0))
          assertThat(data.convictedTransferLimit).isEqualTo(BigDecimal.valueOf(6.5))
          assertThat(data.convictedSpendLimit).isEqualTo(BigDecimal.valueOf(66.0))
        },
      )

      verify(visitAllowanceLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(true)
          assertThat(data.expiryDate).isNull()
          assertThat(data.visitOrderAllowance).isEqualTo(3)
          assertThat(data.privilegedVisitOrderAllowance).isEqualTo(4)
        },
      )
    }

    @Test
    fun `expiry date is set for inactive entities`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(incentivesCodeRepository.findById(ReferenceCode.Pk("IEP_LEVEL", "NSTD"))).thenReturn(
        Optional.of(IEPLevel("NSTD", "desc", true, 1)),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.empty(),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.empty(),
      )
      whenever(prisonIncentiveLevelRepository.save(any())).thenReturn(getPrisonIncentiveLevel())
      whenever(visitAllowanceLevelRepository.save(any())).thenReturn(getVisitAllowanceLevel())
      incentivesService.createPrisonIncentiveLevelData(
        "MDI",
        CreatePrisonIncentiveRequest(
          levelCode = "NSTD",
          active = false,
          defaultOnAdmission = true,
          visitOrderAllowance = 3,
          privilegedVisitOrderAllowance = 4,
          remandTransferLimitInPence = 350,
          remandSpendLimitInPence = 3700,
          convictedTransferLimitInPence = 650,
          convictedSpendLimitInPence = 6600,
        ),
      )

      verify(prisonIncentiveLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(false)
          assertThat(data.expiryDate).isEqualTo(LocalDate.now())
        },
      )

      verify(visitAllowanceLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(false)
          assertThat(data.expiryDate).isEqualTo(LocalDate.now())
        },
      )
    }
  }

  @DisplayName("create prison incentive level data")
  @Nested
  internal inner class UpdatePrisonIncentiveLevelData {
    val prison = AgencyLocation("MDI", "desc")

    @Test
    fun `update handles no existing Visit Allowance data by creating the visit allowance`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(incentivesCodeRepository.findById(ReferenceCode.Pk("IEP_LEVEL", "NSTD"))).thenReturn(
        Optional.of(IEPLevel("NSTD", "desc", true, 1)),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.of(getPrisonIncentiveLevel()),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.empty(),
      )
      whenever(visitAllowanceLevelRepository.save(any())).thenReturn(getVisitAllowanceLevel())

      val updatedResponse = incentivesService.updatePrisonIncentiveLevelData(
        "MDI",
        "NSTD",
        UpdatePrisonIncentiveRequest(
          active = true,
          defaultOnAdmission = true,
          visitOrderAllowance = 3,
          privilegedVisitOrderAllowance = 4,
          remandTransferLimitInPence = 350,
          remandSpendLimitInPence = 3700,
          convictedTransferLimitInPence = 650,
          convictedSpendLimitInPence = 6600,
        ),
      )

      assertThat(updatedResponse.remandTransferLimitInPence).isEqualTo(350)
      assertThat(updatedResponse.visitOrderAllowance).isEqualTo(3)
      assertThat(updatedResponse.privilegedVisitOrderAllowance).isEqualTo(4)
    }

    @Test
    fun `expiry date is set for inactive entities`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(incentivesCodeRepository.findById(ReferenceCode.Pk("IEP_LEVEL", "NSTD"))).thenReturn(
        Optional.of(IEPLevel("NSTD", "desc", true, 1)),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.empty(),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.empty(),
      )
      whenever(prisonIncentiveLevelRepository.save(any())).thenReturn(getPrisonIncentiveLevel())
      whenever(visitAllowanceLevelRepository.save(any())).thenReturn(getVisitAllowanceLevel())
      incentivesService.createPrisonIncentiveLevelData(
        "MDI",
        CreatePrisonIncentiveRequest(
          levelCode = "NSTD",
          active = false,
          defaultOnAdmission = true,
          visitOrderAllowance = 3,
          privilegedVisitOrderAllowance = 4,
          remandTransferLimitInPence = 350,
          remandSpendLimitInPence = 3700,
          convictedTransferLimitInPence = 650,
          convictedSpendLimitInPence = 6600,
        ),
      )

      verify(prisonIncentiveLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(false)
          assertThat(data.expiryDate).isEqualTo(LocalDate.now())
        },
      )

      verify(visitAllowanceLevelRepository).save(
        org.mockito.kotlin.check { data ->
          assertThat(data.active).isEqualTo(false)
          assertThat(data.expiryDate).isEqualTo(LocalDate.now())
        },
      )
    }

    @Test
    fun `data isn't created if entities already exist`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(incentivesCodeRepository.findById(ReferenceCode.Pk("IEP_LEVEL", "NSTD"))).thenReturn(
        Optional.of(IEPLevel("NSTD", "desc", true, 1)),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.of(getPrisonIncentiveLevel()),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.of(getVisitAllowanceLevel()),
      )
      incentivesService.createPrisonIncentiveLevelData(
        "MDI",
        CreatePrisonIncentiveRequest(
          levelCode = "NSTD",
          active = false,
          defaultOnAdmission = true,
          visitOrderAllowance = 3,
          privilegedVisitOrderAllowance = 4,
          remandTransferLimitInPence = 350,
          remandSpendLimitInPence = 3700,
          convictedTransferLimitInPence = 650,
          convictedSpendLimitInPence = 6600,
        ),
      )

      verify(prisonIncentiveLevelRepository, never()).save(any())
      verify(visitAllowanceLevelRepository, never()).save(any())
    }
  }

  @DisplayName("get prison incentive level data")
  @Nested
  internal inner class GetPrisonIncentiveLevelData {
    val prison = AgencyLocation("MDI", "desc")

    @Test
    fun `handles no existing Visit Allowance data`() {
      whenever(agencyLocationRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )
      whenever(prisonIncentiveLevelRepository.findById(PrisonIepLevel.Companion.PK("NSTD", prison))).thenReturn(
        Optional.of(getPrisonIncentiveLevel()),
      )
      whenever(visitAllowanceLevelRepository.findById(VisitAllowanceLevelId(prison, "NSTD"))).thenReturn(
        Optional.empty(),
      )

      val response = incentivesService.getPrisonIncentiveLevel(
        "MDI",
        "NSTD",
      )

      assertThat(response.visitAllowanceExpiryDate).isNull()
      assertThat(response.visitAllowanceActive).isNull()
      assertThat(response.visitOrderAllowance).isNull()
      assertThat(response.privilegedVisitOrderAllowance).isNull()
    }
  }

  private fun getPrisonIncentiveLevel(): PrisonIepLevel {
    val prison = AgencyLocation("MDI", "desc")
    val iepLevel = IEPLevel("STD", "STD-desc")

    return PrisonIepLevel(
      iepLevelCode = iepLevel.code,
      agencyLocation = prison,
      active = false,
      default = false,
      remandTransferLimit = BigDecimal.valueOf(3.5),
      remandSpendLimit = BigDecimal.valueOf(0.5),
      convictedTransferLimit = BigDecimal.valueOf(45.5),
      convictedSpendLimit = BigDecimal.valueOf(4.5),
      iepLevel = iepLevel,
    )
  }

  private fun getVisitAllowanceLevel(): VisitAllowanceLevel {
    val prison = AgencyLocation("MDI", "desc")
    return VisitAllowanceLevel(
      id = VisitAllowanceLevelId(location = prison, iepLevelCode = "STD"),
      visitOrderAllowance = 3,
      privilegedVisitOrderAllowance = 4,
      active = true,
      expiryDate = null,
    )
  }
}

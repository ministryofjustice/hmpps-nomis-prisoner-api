package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class VisitBalanceServiceTest {
  private lateinit var visitBalanceService: VisitBalanceService

  @Mock
  lateinit var offenderBookingRepository: OffenderBookingRepository

  @BeforeEach
  fun setUp() {
    visitBalanceService = VisitBalanceService(
      offenderBookingRepository = offenderBookingRepository,
      visitBalanceRepository = Mockito.mock(),
      offenderVisitBalanceAdjustmentRepository = Mockito.mock(),
    )
  }

  @Nested
  inner class GetVisitOrderBalance {
    @Nested
    @DisplayName("With no visit balance on latest booking")
    inner class WithNoVisitBalanceAtAll {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking())
      }

      @Test
      fun `there will be no last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")

        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isNull()
      }
    }

    @Nested
    @DisplayName("With no visit balance adjustments on latest booking")
    inner class WithNoVisitBalanceAdjustmentsAtAll {
      @BeforeEach
      fun setUp() {
        val booking = booking()
        val ovb = OffenderVisitBalance(
          remainingVisitOrders = 10,
          remainingPrivilegedVisitOrders = 5,
          offenderBooking = booking,
        )
        booking.visitBalance = ovb

        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `there will be no last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")

        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isNull()
      }
    }

    @Nested
    @DisplayName("With no IEP visit balance adjustments on latest booking")
    inner class WithNoIEPVisitBalanceAdjustments {
      @BeforeEach
      fun setUp() {
        val booking = booking()
        val ovb = OffenderVisitBalance(
          remainingVisitOrders = 10,
          remainingPrivilegedVisitOrders = 5,
          offenderBooking = booking,
        )
        booking.visitBalance = ovb
        booking.visitBalanceAdjustments.add(
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE, "VO Issue"),
            adjustDate = LocalDate.now(),
            offenderBooking = booking,
          ),
        )

        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `there will be no last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")

        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isNull()
      }
    }

    @Nested
    @DisplayName("With 1 IEP visit balance adjustment not from batch allocation on latest booking")
    inner class WithOneNonBatchIEPVisitBalanceAdjustments {
      @BeforeEach
      fun setUp() {
        val booking = booking()
        val ovb = OffenderVisitBalance(
          remainingVisitOrders = 10,
          remainingPrivilegedVisitOrders = 5,
          offenderBooking = booking,
        )
        booking.visitBalance = ovb
        booking.visitBalanceAdjustments.add(
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(IEP_ENTITLEMENT, "IEP Entitlement"),
            adjustDate = LocalDate.now(),
            offenderBooking = booking,
          ).apply { createUsername = "NOT_BATCH_JOB" },
        )

        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")
        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isEqualTo("2025-03-13")
      }
    }

    @Nested
    @DisplayName("With 1 IEP visit balance adjustment on latest booking")
    inner class WithOneIEPVisitBalanceAdjustments {
      @BeforeEach
      fun setUp() {
        val booking = booking()
        val ovb = OffenderVisitBalance(
          remainingVisitOrders = 10,
          remainingPrivilegedVisitOrders = 5,
          offenderBooking = booking,
        )
        booking.visitBalance = ovb
        booking.visitBalanceAdjustments.add(
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(IEP_ENTITLEMENT, "IEP Entitlement"),
            adjustDate = LocalDate.now(),
            offenderBooking = booking,
          ).apply { createUsername = "OMS_OWNER" },
        )

        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")

        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.now())
      }
    }

    @Nested
    @DisplayName("With multiple IEP visit balance adjustments on latest booking")
    inner class WithMultipleIEPVisitBalanceAdjustments {
      @BeforeEach
      fun setUp() {
        val booking = booking()
        val ovb = OffenderVisitBalance(
          remainingVisitOrders = 10,
          remainingPrivilegedVisitOrders = 5,
          offenderBooking = booking,
        )
        booking.visitBalance = ovb
        val visitBalanceAdjustments = listOf(
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(IEP_ENTITLEMENT, "IEP Entitlement"),
            adjustDate = LocalDate.parse("2021-01-01"),
            offenderBooking = booking,
          ).apply { createUsername = "OMS_OWNER" },
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(IEP_ENTITLEMENT, "IEP Entitlement"),
            adjustDate = LocalDate.parse("2023-02-03"),
            offenderBooking = booking,
          ).apply { createUsername = "OMS_OWNER" },
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(PVO_IEP_ENTITLEMENT, "PVO IEP Entitlement"),
            adjustDate = LocalDate.parse("2023-02-03"),
            offenderBooking = booking,
          ).apply { createUsername = "OMS_OWNER" },
          OffenderVisitBalanceAdjustment(
            adjustReasonCode = VisitOrderAdjustmentReason(IEP_ENTITLEMENT, "IEP Entitlement"),
            adjustDate = LocalDate.parse("2023-02-02"),
            offenderBooking = booking,
          ).apply { createUsername = "OMS_OWNER" },
        )
        booking.visitBalanceAdjustments.addAll(visitBalanceAdjustments)

        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitOrderBalance("A1234KT")

        Assertions.assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.parse("2023-02-03"))
      }
    }
  }
}

private fun booking(bookingSequence: Int = 1): OffenderBooking = OffenderBooking(
  bookingId = 123,
  bookingSequence = bookingSequence,
  bookingBeginDate = LocalDateTime.now(),
  offender = Offender(nomsId = "A1234KT", gender = Gender("M", "MALE"), lastName = "SMITH", firstName = "JOHN"),
)

package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class VisitBalanceServiceTest {
  private lateinit var visitBalanceService: VisitBalanceService

  @Mock
  lateinit var offenderBookingRepository: OffenderBookingRepository

  @BeforeEach
  fun setUp() {
    visitBalanceService = VisitBalanceService(
      offenderBookingRepository = offenderBookingRepository,
      visitBalanceRepository = mock(),
      offenderVisitBalanceAdjustmentRepository = mock(),
    )
  }

  @Nested
  inner class GetVisitBalanceDetail {
    @Nested
    @DisplayName("With no booking")
    inner class WithNoVisitBookingAtAll {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.ofNullable(null))
      }

      @Test
      fun `will throw exception`() {
        assertThrows(NotFoundException::class.java) {
          visitBalanceService.getVisitBalanceById(123)
        }
      }
    }

    @Nested
    @DisplayName("With no visit balance on latest booking")
    inner class WithNoVisitBalanceAtAll {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking()))
      }

      @Test
      fun `will throw exception`() {
        assertThrows(NotFoundException::class.java) {
          visitBalanceService.getVisitBalanceById(123)
        }
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

        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking))
      }

      @Test
      fun `there will be no last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitBalanceById(123)

        assertThat(visitBalance.lastIEPAllocationDate).isNull()
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

        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking))
      }

      @Test
      fun `there will be no last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitBalanceById(123)

        assertThat(visitBalance.lastIEPAllocationDate).isNull()
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

        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking))
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitBalanceById(123)
        assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.now().toString())
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

        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking))
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitBalanceById(123)

        assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.now())
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

        whenever(offenderBookingRepository.findById(anyLong())).thenReturn(Optional.of(booking))
      }

      @Test
      fun `there will be a last IEP allocation date`() {
        val visitBalance = visitBalanceService.getVisitBalanceById(123)

        assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.parse("2023-02-03"))!!
      }
    }
  }

  @Nested
  inner class GetVisitBalanceForOffender {
    @Nested
    @DisplayName("With no booking")
    inner class WithNoAssociatedBooking {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(null)
      }

      @Test
      fun `there will be no visit data set`() {
        assertThrows(NotFoundException::class.java) {
          visitBalanceService.getVisitBalanceForPrisoner("A1234KT")
        }
      }
    }

    @Nested
    @DisplayName("With no visit balance on latest booking")
    inner class WithNoVisitBalanceAtAll {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking())
      }

      @Test
      fun `there will be no visit data set`() {
        val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1234KT")
        assertThat(visitBalance).isNull()
      }
    }

    @Nested
    @DisplayName("With visit balance on latest booking")
    inner class WithVisitBalance {
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
      fun `there will be visit data set`() {
        val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1234KT")!!

        assertThat(visitBalance.remainingVisitOrders).isEqualTo(10)
        assertThat(visitBalance.remainingPrivilegedVisitOrders).isEqualTo(5)
      }
    }
  }
}

private fun booking(bookingSequence: Int = 1): OffenderBooking = OffenderBooking(
  bookingId = 123,
  bookingSequence = bookingSequence,
  bookingBeginDate = LocalDateTime.parse("2025-03-13T01:02:03"),
  offender = Offender(nomsId = "A1234KT", gender = Gender("M", "MALE"), lastName = "SMITH", firstName = "JOHN"),
)

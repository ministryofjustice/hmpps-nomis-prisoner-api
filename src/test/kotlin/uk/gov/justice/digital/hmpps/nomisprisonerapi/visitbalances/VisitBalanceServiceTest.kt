package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.description
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_ISSUE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.VO_ISSUE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances.CreateVisitBalanceAdjustmentRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class VisitBalanceServiceTest {
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason> = mock()
  private val staffUserAccountRepository: StaffUserAccountRepository = mock()
  private val visitBalanceService = VisitBalanceService(
    offenderBookingRepository = offenderBookingRepository,
    visitBalanceRepository = mock(),
    offenderVisitBalanceAdjustmentRepository = mock(),
    visitOrderAdjustmentReasonRepository = visitOrderAdjustmentReasonRepository,
    staffUserAccountRepository = staffUserAccountRepository,
  )

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

        assertThat(visitBalance.lastIEPAllocationDate).isEqualTo(LocalDate.parse("2023-02-03"))
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
    @DisplayName("With null entries in visit balance on latest booking")
    inner class WithNullEntriesInVisitBalance {

      @Nested
      @DisplayName("With null entries in visit balance on latest booking")
      inner class WithNullEntriesForBothFields {
        @BeforeEach
        fun setUp() {
          val booking = booking()
          val ovb = OffenderVisitBalance(
            remainingVisitOrders = null,
            remainingPrivilegedVisitOrders = null,
            offenderBooking = booking,
          )
          booking.visitBalance = ovb
          whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
        }

        @Test
        fun `there will be no visit data set if null entries for both visit balance entries`() {
          val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1234KT")
          assertThat(visitBalance).isNull()
        }
      }

      @Nested
      @DisplayName("With null entry for visit balance visit balance on latest booking")
      inner class WithNullEntryForVisitBalance {
        @BeforeEach
        fun setUp() {
          val booking = booking()
          val ovb = OffenderVisitBalance(
            remainingVisitOrders = null,
            remainingPrivilegedVisitOrders = 5,
            offenderBooking = booking,
          )
          booking.visitBalance = ovb
          whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
        }

        @Test
        fun `there will be no visit data set if null entry for visit order balance`() {
          val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1234KT")
          assertThat(visitBalance).isNull()
        }
      }

      @Nested
      @DisplayName("With null entry for privileged visit balance visit balance on latest booking")
      inner class WithNullEntryForPrivilegedVisitBalance {
        @BeforeEach
        fun setUp() {
          val booking = booking()
          val ovb = OffenderVisitBalance(
            remainingVisitOrders = 5,
            remainingPrivilegedVisitOrders = null,
            offenderBooking = booking,
          )
          booking.visitBalance = ovb
          whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1235KT")).thenReturn(booking)
        }

        @Test
        fun `there will be no visit data set if null entry for privileged visit order balance`() {
          val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1235KT")
          assertThat(visitBalance).isNull()
        }
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
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1236KT")).thenReturn(booking)
      }

      @Test
      fun `there will be visit data set`() {
        val visitBalance = visitBalanceService.getVisitBalanceForPrisoner("A1236KT")!!

        assertThat(visitBalance.remainingVisitOrders).isEqualTo(10)
        assertThat(visitBalance.remainingPrivilegedVisitOrders).isEqualTo(5)
      }
    }
  }

  @Nested
  inner class CreateVisitBalanceAdjustment {
    @Nested
    @DisplayName("With no booking")
    inner class WithNoAssociatedBooking {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(null)
      }

      @Test
      fun `it will throw a not found exception`() {
        assertThrows(NotFoundException::class.java) {
          visitBalanceService.createVisitBalanceAdjustment("A1234KT", CreateVisitBalanceAdjustmentRequest(adjustmentDate = LocalDate.now()))
        }
      }
    }

    @Nested
    @DisplayName("With missing adjustment reason")
    inner class WithMissingAdjustmentReason {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking())
      }

      @Test
      fun `it will throw a runtime exception if no VO_ISSUE`() {
        assertThatThrownBy {
          visitBalanceService.createVisitBalanceAdjustment("A1234KT", CreateVisitBalanceAdjustmentRequest(adjustmentDate = LocalDate.now(), visitOrderChange = 1))
        }.isInstanceOf(RuntimeException::class.java).hasMessageStartingWith("Visit Adjustment Reason with code").hasMessageContaining("code=VO_ISSUE")
      }

      @Test
      fun `it will throw a runtime exception if no PVO_ISSUE`() {
        assertThatThrownBy {
          visitBalanceService.createVisitBalanceAdjustment("A1234KT", CreateVisitBalanceAdjustmentRequest(adjustmentDate = LocalDate.now()))
        }.isInstanceOf(RuntimeException::class.java).hasMessageStartingWith("Visit Adjustment Reason with code").hasMessageContaining("code=PVO_ISSUE")
      }
    }

    @Nested
    @DisplayName("With missing user")
    inner class MissingAuthorisedUser {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking())

        whenever(visitOrderAdjustmentReasonRepository.findById(any())).thenReturn(
          Optional.of(
            VisitOrderAdjustmentReason(
              code = "CODE",
              description = "DESC",
            ),
          ),
        )
      }

      @Test
      fun `it will throw a bad data exception when user supplied`() {
        assertThatThrownBy {
          visitBalanceService.createVisitBalanceAdjustment("A1234KT", CreateVisitBalanceAdjustmentRequest(adjustmentDate = LocalDate.now(), authorisedUsername = "BILLY"))
        }.isInstanceOf(BadDataException::class.java).hasMessage("Username BILLY not found")
      }
    }

    @Nested
    @DisplayName("With create balance adjustment on booking")
    inner class WithVisitBalance {
      private lateinit var booking: OffenderBooking

      @BeforeEach
      fun setUp() {
        booking = booking().apply {
          visitBalance = OffenderVisitBalance(
            remainingVisitOrders = 10,
            remainingPrivilegedVisitOrders = 5,
            offenderBooking = this,
          )
        }
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1236KT")).thenReturn(booking)

        whenever(staffUserAccountRepository.findByUsername(any())).thenReturn(
          StaffUserAccount(
            username = "JMORROW_GEN",
            Staff(12345L, "First1", "Last1"),
            "type",
            "source",
          ),
        )
        whenever(visitOrderAdjustmentReasonRepository.findById(any())).thenReturn(
          Optional.of(
            VisitOrderAdjustmentReason(
              code = "CODE",
              description = "DESC",
            ),
          ),
        )
      }

      @Test
      fun `it will create a balance adjustment for specified staff user`() {
        visitBalanceService.createVisitBalanceAdjustment(
          "A1236KT",
          CreateVisitBalanceAdjustmentRequest(
            visitOrderChange = 1,
            privilegedVisitOrderChange = 2,
            adjustmentDate = LocalDate.now(),
            authorisedUsername = "JMORROW_GEN",
          ),
        )
        assertThat(booking.visitBalanceAdjustments).hasSize(1)
        verify(staffUserAccountRepository).findByUsername("JMORROW_GEN")
      }

      @Test
      fun `it will create a balance adjustment for system user`() {
        visitBalanceService.createVisitBalanceAdjustment(
          "A1236KT",
          CreateVisitBalanceAdjustmentRequest(
            visitOrderChange = 1,
            privilegedVisitOrderChange = 2,
            adjustmentDate = LocalDate.now(),
          ),
        )
        assertThat(booking.visitBalanceAdjustments).hasSize(1)
        verify(staffUserAccountRepository).findByUsername("OMS_OWNER")
      }

      @Test
      fun `it will create a VO_ISSUE balance adjustment when visit order change supplied`() {
        visitBalanceService.createVisitBalanceAdjustment(
          "A1236KT",
          CreateVisitBalanceAdjustmentRequest(
            visitOrderChange = 1,
            privilegedVisitOrderChange = 2,
            adjustmentDate = LocalDate.now(),
          ),
        )
        assertThat(booking.visitBalanceAdjustments).hasSize(1)
        verify(visitOrderAdjustmentReasonRepository).findById(VO_ISSUE)
      }

      @Test
      fun `it will create a PVO_ISSUE balance adjustment when visit order change not supplied`() {
        visitBalanceService.createVisitBalanceAdjustment(
          "A1236KT",
          CreateVisitBalanceAdjustmentRequest(
            privilegedVisitOrderChange = 2,
            adjustmentDate = LocalDate.now(),
          ),
        )
        assertThat(booking.visitBalanceAdjustments).hasSize(1)
        verify(visitOrderAdjustmentReasonRepository).findById(PVO_ISSUE)
      }
    }
  }

  @Nested
  inner class UpsertVisitBalance {
    @Nested
    @DisplayName("With no booking")
    inner class WithNoAssociatedBooking {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(null)
      }

      @Test
      fun `it will throw a not found exception`() {
        assertThrows(NotFoundException::class.java) {
          visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(null, null))
        }
      }
    }

    @Nested
    @DisplayName("With no existing visit balance")
    inner class WithNoExistingBalance {
      val booking = booking()

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `it will do nothing if no entries passed in`() {
        visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(null, null))
        assertThat(booking.visitBalance).isNull()
      }

      @Test
      fun `it will update the balance if vo passed in`() {
        visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(remainingVisitOrders = 5, remainingPrivilegedVisitOrders = null))
        assertThat(booking.visitBalance?.remainingVisitOrders).isEqualTo(5)
        assertThat(booking.visitBalance?.remainingPrivilegedVisitOrders).isNull()
      }

      @Test
      fun `it will update the balance if pvo passed in`() {
        visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(remainingVisitOrders = null, remainingPrivilegedVisitOrders = 5))
        assertThat(booking.visitBalance?.remainingVisitOrders).isNull()
        assertThat(booking.visitBalance?.remainingPrivilegedVisitOrders).isEqualTo(5)
      }
    }

    @Nested
    @DisplayName("With existing visit balance")
    inner class WithExistingBalance {
      val booking = booking().apply { this.visitBalance = OffenderVisitBalance(offenderBooking = this) }

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findLatestByOffenderNomsId("A1234KT")).thenReturn(booking)
      }

      @Test
      fun `it will reset any existing balance if no balance passed in`() {
        booking.visitBalance?.remainingVisitOrders = 5
        booking.visitBalance?.remainingPrivilegedVisitOrders = 4
        visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(null, null))
        assertThat(booking.visitBalance?.remainingVisitOrders).isNull()
        assertThat(booking.visitBalance?.remainingPrivilegedVisitOrders).isNull()
      }

      @Test
      fun `it will update the balance`() {
        visitBalanceService.upsertVisitBalance("A1234KT", UpdateVisitBalanceRequest(remainingVisitOrders = 5, remainingPrivilegedVisitOrders = null))
        assertThat(booking.visitBalance?.remainingVisitOrders).isEqualTo(5)
        assertThat(booking.visitBalance?.remainingPrivilegedVisitOrders).isNull()
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

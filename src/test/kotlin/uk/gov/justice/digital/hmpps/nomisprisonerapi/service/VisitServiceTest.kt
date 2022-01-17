@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

private const val offenderBookingId = -9L
private const val visitId = -8L
private const val offenderNo = "A1234AA"
private const val prisonId = "SWI"

internal class VisitServiceTest {

  private val visitRepository: VisitRepository = mock()
  private val visitVisitorRepository: VisitVisitorRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository = mock()
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository = mock()
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus> = mock()
  private val visitTypeRepository: ReferenceCodeRepository<VisitType> = mock()
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus> = mock()
  private val agencyRepository: AgencyLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val visitService: VisitService = VisitService(
    visitRepository,
    visitVisitorRepository,
    offenderBookingRepository,
    offenderVisitBalanceRepository,
    offenderVisitBalanceAdjustmentRepository,
    eventStatusRepository,
    visitTypeRepository,
    visitStatusRepository,
    agencyRepository,
    telemetryClient,
  )

  val visitType = VisitType("SCON", "desc")
  val visitStatus = VisitStatus("SCH", "desc")
  val eventStatus = EventStatus("SCH", "desc")

  val createVisitRequest = CreateVisitRequest(
    visitType = "SCON",
    offenderNo = offenderNo,
    startTime = LocalDateTime.of(2021, 11, 4, 12, 5),
    endTime = LocalTime.of(13, 4),
    prisonId = prisonId,
    visitorPersonIds = listOf(45L, 46L),
    decrementBalances = true,
  )

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
      Optional.of(
        OffenderBooking(
          bookingId = offenderBookingId,
          bookingBeginDate = LocalDateTime.now(),
        )
      )
    )
    whenever(visitTypeRepository.findById(VisitType.pk("SCON"))).thenReturn(Optional.of(visitType))
    whenever(visitStatusRepository.findById(VisitStatus.pk("SCH"))).thenReturn(Optional.of(visitStatus))
    whenever(eventStatusRepository.findById(EventStatus.SCHEDULED_APPROVED)).thenReturn(Optional.of(eventStatus))
    whenever(agencyRepository.findById(prisonId)).thenReturn(Optional.of(AgencyLocation(prisonId, "desc")))

    whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(
      Optional.of(
        OffenderVisitBalance(
          offenderBookingId = offenderBookingId,
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 5,
        )
      )
    )
  }

  @Nested
  internal inner class createVisit {
    @Test
    fun success() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      assertThat(visitService.createVisit(createVisitRequest)).isEqualTo(CreateVisitResponse(visitId))

      val balanceArgumentCaptor = ArgumentCaptor.forClass(OffenderVisitBalanceAdjustment::class.java)
      verify(offenderVisitBalanceAdjustmentRepository).save(balanceArgumentCaptor.capture())
      val balanceArgument = balanceArgumentCaptor.value
      assertThat(balanceArgument.adjustReasonCode).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
      assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
      assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
      assertThat(balanceArgument.commentText).isEqualTo("Created by PVB3 for an on-line visit booking")

      val visitArgumentCaptor = ArgumentCaptor.forClass(Visit::class.java)
      verify(visitRepository).save(visitArgumentCaptor.capture())
      val visit = visitArgumentCaptor.value
      assertThat(visit.visitDate).isEqualTo(LocalDate.of(2021, 11, 4))
      assertThat(visit.startTime).isEqualTo(LocalDateTime.of(2021, 11, 4, 12, 5))
      assertThat(visit.endTime).isEqualTo(LocalDateTime.of(2021, 11, 4, 13, 4))
      assertThat(visit.visitType).isEqualTo(visitType)
      assertThat(visit.visitStatus).isEqualTo(visitStatus)
      assertThat(visit.location?.id).isEqualTo(prisonId)

      val inOrder = Mockito.inOrder(visitVisitorRepository)
      val visitorArgumentCaptor = ArgumentCaptor.forClass(VisitVisitor::class.java)
      inOrder.verify(visitVisitorRepository, times(3)).save(visitorArgumentCaptor.capture())
      assertThat(visitorArgumentCaptor.allValues).extracting(
        "visitId", "offenderBooking.bookingId", "personId", "eventStatus"
      ).containsExactly(
        Tuple.tuple(visitId, offenderBookingId, null, eventStatus),
        Tuple.tuple(visitId, null, 45L, eventStatus),
        Tuple.tuple(visitId, null, 46L, eventStatus),
      )
    }

    @Test
    fun successWithPrivilege() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      assertThat(visitService.createVisit(createVisitRequest.copy(privileged = true))).isEqualTo(CreateVisitResponse(visitId))

      val balanceArgumentCaptor = ArgumentCaptor.forClass(OffenderVisitBalanceAdjustment::class.java)
      verify(offenderVisitBalanceAdjustmentRepository).save(balanceArgumentCaptor.capture())
      val balanceArgument = balanceArgumentCaptor.value
      assertThat(balanceArgument.adjustReasonCode).isEqualTo(VisitOrderAdjustmentReason.PVO_ISSUE)
      assertThat(balanceArgument.remainingVisitOrders).isNull()
      assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
      assertThat(balanceArgument.commentText).isEqualTo("Created by PVB3 for an on-line visit booking")
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
        Optional.empty()
      )

      val thrown = assertThrows(PrisonerNotFoundException::class.java) {
        visitService.createVisit(createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo(offenderNo)
    }
  }
}

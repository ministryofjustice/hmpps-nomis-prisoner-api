@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOutcomeReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
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
private const val roomId = "VISIT-ROOM"
private const val eventId = 34L
private const val visitOrder = 54L

internal class VisitServiceTest {

  private val visitRepository: VisitRepository = mock()
  private val visitVisitorRepository: VisitVisitorRepository = mock()
  private val visitOrderRepository: VisitOrderRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val personRepository: PersonRepository = mock()
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository = mock()
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository = mock()
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus> = mock()
  private val visitTypeRepository: ReferenceCodeRepository<VisitType> = mock()
  private val visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType> = mock()
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus> = mock()
  private val visitOutcomeRepository: ReferenceCodeRepository<VisitOutcomeReason> = mock()
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome> = mock()
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason> = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val visitService: VisitService = VisitService(
    visitRepository,
    visitVisitorRepository,
    visitOrderRepository,
    offenderBookingRepository,
    offenderVisitBalanceRepository,
    offenderVisitBalanceAdjustmentRepository,
    eventStatusRepository,
    visitTypeRepository,
    visitOrderTypeRepository,
    visitStatusRepository,
    visitOutcomeRepository,
    eventOutcomeRepository,
    visitOrderAdjustmentReasonRepository,
    agencyLocationRepository,
    agencyInternalLocationRepository,
    telemetryClient,
    personRepository
  )

  val visitType = VisitType("SCON", "desc")

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
      Optional.of(OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now()))
    )
    whenever(personRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There"))
    }
    whenever(visitTypeRepository.findById(VisitType.pk("SCON"))).thenReturn(Optional.of(visitType))
    whenever(visitStatusRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(VisitStatus((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(eventStatusRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(EventStatus((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(visitOrderAdjustmentReasonRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(VisitOrderAdjustmentReason((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(visitOrderTypeRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(VisitOrderType((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(visitOutcomeRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(VisitOutcomeReason((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(eventOutcomeRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(EventOutcome((it.arguments[0] as ReferenceCode.Pk).code!!, "desc"))
    }
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.of(AgencyLocation(prisonId, "desc")))
    whenever(agencyInternalLocationRepository.findByLocationCodeAndAgencyId(roomId, prisonId)).thenReturn(
      listOf(AgencyInternalLocation(12345L, true))
    )

    whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(
      Optional.of(
        OffenderVisitBalance(
          offenderBooking = OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now()),
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 5,
        )
      )
    )

    whenever(visitVisitorRepository.getEventId()).thenReturn(eventId)
    whenever(visitOrderRepository.getVisitOrderNumber()).thenReturn(visitOrder)
  }

  @DisplayName("create")
  @Nested
  internal inner class CreateVisit {
    val createVisitRequest = CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = prisonId,
      visitorPersonIds = listOf(45L, 46L),
      visitRoomId = roomId,
      vsipVisitId = "12345",
      issueDate = LocalDate.parse("2021-11-02"),
    )

    @Test
    fun `visit data is mapped correctly`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      assertThat(visitService.createVisit(offenderNo, createVisitRequest)).isEqualTo(CreateVisitResponse(visitId))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitDate).isEqualTo(LocalDate.parse("2021-11-04"))
          assertThat(visit.startTime).isEqualTo(LocalDateTime.parse("2021-11-04T12:05"))
          assertThat(visit.endTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
          assertThat(visit.visitType).isEqualTo(visitType)
          assertThat(visit.visitStatus?.code).isEqualTo("SCH")
          assertThat(visit.location?.id).isEqualTo(prisonId)
          assertThat(visit.vsipVisitId).isEqualTo(VisitService.VSIP_PREFIX + "12345")
        }
      )
    }

    @Test
    fun `balance decrement is saved correctly when no privileged is available`() {

      whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(
        Optional.of(
          OffenderVisitBalance(
            remainingVisitOrders = 3,
            remainingPrivilegedVisitOrders = 0,
            offenderBooking = OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now()),
          )
        )
      )

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP for an on-line visit booking")
        }
      )
    }

    @Test
    fun `privilege balance decrement is saved correctly when available`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP for an on-line visit booking")
        }
      )
    }

    @Test
    fun `No visit order or balance adjustment is created when no balance available`() {

      whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(
        Optional.of(
          OffenderVisitBalance(
            remainingVisitOrders = 0,
            remainingPrivilegedVisitOrders = 0,
            offenderBooking = OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now()),
          )
        )
      )
      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNull() })
      verify(offenderVisitBalanceAdjustmentRepository, times(0)).save(any())
    }

    @Test
    fun `No visit order or balance adjustment is created when no balance record exists`() {

      whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(Optional.empty())
      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNull() })
      verify(offenderVisitBalanceAdjustmentRepository, times(0)).save(any())
    }

    @Test
    fun `visitor records are saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitors).extracting("offenderBooking.bookingId", "person.id", "eventStatus.code", "eventId")
            .containsExactly(
              Tuple.tuple(offenderBookingId, null, "SCH", eventId),
              Tuple.tuple(null, 45L, "SCH", null),
              Tuple.tuple(null, 46L, "SCH", null),
            )
        }
      )
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
        Optional.empty()
      )

      val thrown = assertThrows(PrisonerNotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo(offenderNo)
    }

    @Test
    fun personNotFound() {
      whenever(personRepository.findById(45L)).thenReturn(Optional.empty())

      val thrown = assertThrows(DataNotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Person with id=45 does not exist")
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows(DataNotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }

    @Test
    fun roomNotFound() {
      whenever(agencyInternalLocationRepository.findByLocationCodeAndAgencyId(roomId, prisonId)).thenReturn(emptyList())

      val thrown = assertThrows(DataNotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Room location with code=$roomId does not exist in prison $prisonId")
    }

    @Test
    fun moreThanOneRoom() {
      whenever(agencyInternalLocationRepository.findByLocationCodeAndAgencyId(roomId, prisonId)).thenReturn(
        listOf(AgencyInternalLocation(12345L, true), AgencyInternalLocation(12346L, true))
      )

      val thrown = assertThrows(DataNotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("There is more than one room with code=$roomId at prison $prisonId")
    }
  }

  @DisplayName("cancel")
  @Nested
  internal inner class CancelVisit {
    val cancelVisitRequest = CancelVisitRequest(outcome = "OFFCANC")
    val offenderBooking = OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now())
    val visit = Visit(
      id = visitId,
      visitStatus = VisitStatus("SCH", "desc"),
      offenderBooking = offenderBooking,
      visitOrder = VisitOrder(
        offenderBooking = offenderBooking,
        visitOrderNumber = 123L,
        visitOrderType = VisitOrderType("PVO", "desc"),
        status = VisitStatus("SCH", "desc"),
        issueDate = LocalDate.parse("2021-12-01"),
      ),
    )

    init {
      visit.visitors.add(VisitVisitor(offenderBooking = offenderBooking, visit = visit))
      visit.visitors.add(VisitVisitor(person = Person(-7L, "First", "Last"), visit = visit))
    }

    @Test
    fun `visit data is amended correctly`() {

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      with(visit) {
        assertThat(visitStatus?.code).isEqualTo("CANC")

        assertThat(visitors).extracting("eventOutcome.code", "eventStatus.code", "outcomeReason.code")
          .containsExactly(
            Tuple.tuple("ABS", "CANC", "OFFCANC"),
            Tuple.tuple("ABS", "CANC", "OFFCANC"),
          )
        with(visitOrder!!) {
          assertThat(status.code).isEqualTo("CANC")
          assertThat(outcomeReason?.code).isEqualTo("OFFCANC")
          assertThat(expiryDate).isEqualTo(LocalDate.now())
        }
      }
    }

    @Test
    fun `balance increment is saved correctly`() {

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_CANCEL)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(1)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Booking cancelled by VSIP")
        }
      )
    }
  }
}

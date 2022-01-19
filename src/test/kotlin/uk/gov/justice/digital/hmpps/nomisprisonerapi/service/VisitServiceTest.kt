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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
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
private const val roomId = "VISIT-ROOM"
private const val eventId = 34L

internal class VisitServiceTest {

  private val visitRepository: VisitRepository = mock()
  private val visitVisitorRepository: VisitVisitorRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val personRepository: PersonRepository = mock()
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository = mock()
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository = mock()
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus> = mock()
  private val visitTypeRepository: ReferenceCodeRepository<VisitType> = mock()
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus> = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
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
    agencyLocationRepository,
    agencyInternalLocationRepository,
    telemetryClient,
    personRepository
  )

  val visitType = VisitType("SCON", "desc")
  val visitStatus = VisitStatus("SCH", "desc")
  val eventStatus = EventStatus("SCH", "desc")

  val createVisitRequest = CreateVisitRequest(
    visitType = "SCON",
    startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
    endTime = LocalTime.parse("13:04"),
    prisonId = prisonId,
    visitorPersonIds = listOf(45L, 46L),
    decrementBalances = true,
    visitRoomId = roomId,
  )

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
      Optional.of(OffenderBooking(bookingId = offenderBookingId, bookingBeginDate = LocalDateTime.now()))
    )
    whenever(personRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There"))
    }
    whenever(visitTypeRepository.findById(VisitType.pk("SCON"))).thenReturn(Optional.of(visitType))
    whenever(visitStatusRepository.findById(VisitStatus.pk("SCH"))).thenReturn(Optional.of(visitStatus))
    whenever(eventStatusRepository.findById(EventStatus.SCHEDULED_APPROVED)).thenReturn(Optional.of(eventStatus))
    whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.of(AgencyLocation(prisonId, "desc")))
    whenever(agencyInternalLocationRepository.findByLocationCodeAndAgencyId(roomId, prisonId)).thenReturn(
      listOf(AgencyInternalLocation(12345L, true))
    )

    whenever(offenderVisitBalanceRepository.findById(offenderBookingId)).thenReturn(
      Optional.of(
        OffenderVisitBalance(
          offenderBookingId = offenderBookingId,
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 5,
        )
      )
    )

    whenever(visitVisitorRepository.getEventId()).thenReturn(eventId)
  }

  @DisplayName("create")
  @Nested
  internal inner class createVisit {
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
          assertThat(visit.visitStatus).isEqualTo(visitStatus)
          assertThat(visit.location?.id).isEqualTo(prisonId)
        }
      )
    }

    @Test
    fun `balance decrement is saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Created by PVB3 for an on-line visit booking")
        }
      )
    }

    @Test
    fun `privilege balance decrement is saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode).isEqualTo(VisitOrderAdjustmentReason.PVO_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.commentText).isEqualTo("Created by PVB3 for an on-line visit booking")
        }
      )
    }

    @Test
    fun `visitor records are saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(Visit(id = visitId))

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitors).extracting("offenderBooking.bookingId", "person.id", "eventStatus", "eventId")
            .containsExactly(
              Tuple.tuple(offenderBookingId, null, eventStatus, eventId),
              Tuple.tuple(null, 45L, eventStatus, eventId),
              Tuple.tuple(null, 46L, eventStatus, eventId),
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
}

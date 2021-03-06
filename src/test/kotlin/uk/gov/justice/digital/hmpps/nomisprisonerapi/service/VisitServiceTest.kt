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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.VisitFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDayId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.VisitSpecification
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

private const val offenderBookingId = -9L
private const val visitId = 1008L
private const val offenderNo = "A1234AA"
private const val prisonId = "SWI"
private const val eventId = 34L
private const val visitOrder = 54L

internal class VisitServiceTest {

  private val visitRepository: VisitRepository = mock()
  private val visitVisitorRepository: VisitVisitorRepository = mock()
  private val visitOrderRepository: VisitOrderRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val personRepository: PersonRepository = mock()
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository = mock()
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus> = mock()
  private val visitTypeRepository: ReferenceCodeRepository<VisitType> = mock()
  private val visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType> = mock()
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus> = mock()
  private val visitDayRepository: AgencyVisitDayRepository = mock()
  private val visitTimeRepository: AgencyVisitTimeRepository = mock()
  private val visitSlotRepository: AgencyVisitSlotRepository = mock()
  private val internalLocationRepository: AgencyInternalLocationRepository = mock()
  private val visitOutcomeRepository: ReferenceCodeRepository<VisitOutcomeReason> = mock()
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome> = mock()
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason> = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val visitService: VisitService = VisitService(
    visitRepository,
    visitVisitorRepository,
    visitOrderRepository,
    offenderBookingRepository,
    offenderVisitBalanceAdjustmentRepository,
    eventStatusRepository,
    visitTypeRepository,
    visitOrderTypeRepository,
    visitStatusRepository,
    visitOutcomeRepository,
    eventOutcomeRepository,
    visitOrderAdjustmentReasonRepository,
    agencyLocationRepository,
    telemetryClient,
    personRepository,
    visitDayRepository,
    visitTimeRepository,
    visitSlotRepository,
    internalLocationRepository
  )

  val visitType = VisitType("SCON", "desc")
  val defaultOffender = Offender(nomsId = offenderNo, lastName = "Smith", gender = Gender("MALE", "Male"))
  private val defaultOffenderBooking = OffenderBooking(
    bookingId = offenderBookingId,
    offender = defaultOffender,
    bookingBeginDate = LocalDateTime.now()
  ).apply { // add circular reference
    visitBalance = OffenderVisitBalance(
      offenderBooking = this,
      remainingVisitOrders = 3,
      remainingPrivilegedVisitOrders = 5,
    )
  }
  val defaultVisit = Visit(
    id = visitId,
    visitStatus = VisitStatus("SCH", "desc"),
    offenderBooking = defaultOffenderBooking,
    visitOrder = VisitOrder(
      offenderBooking = defaultOffenderBooking,
      visitOrderNumber = 123L,
      visitOrderType = VisitOrderType("PVO", "desc"),
      status = VisitStatus("SCH", "desc"),
      issueDate = LocalDate.parse("2021-12-01"),
    ),
    startDateTime = LocalDateTime.parse("2022-01-01T09:00"),
    endDateTime = LocalDateTime.parse("2022-01-01T10:00"),
    visitDate = LocalDate.parse("2022-01-01"),
    location = AgencyLocation(id = "MDI", description = "Moorlands"),
    visitType = VisitType(
      code = "SCON", description = "Social contact"
    ),
    commentText = "some comments",
    visitorConcernText = "concerns"
  ).apply { // add circular reference
    visitors.add(VisitVisitor(visit = this, offenderBooking = defaultOffenderBooking))
    visitors.add(VisitVisitor(visit = this, person = Person(-7L, "First", "Last")))
  }

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
      Optional.of(defaultOffenderBooking)
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

    whenever(visitVisitorRepository.getEventId()).thenReturn(eventId)
    whenever(visitOrderRepository.getVisitOrderNumber()).thenReturn(visitOrder)
  }

  @DisplayName("create")
  @Nested
  internal inner class CreateVisit {
    private val createVisitRequest = CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = prisonId,
      visitorPersonIds = listOf(45L, 46L),
      issueDate = LocalDate.parse("2021-11-02"),
    )

    @Test
    fun `visit data is mapped correctly`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      assertThat(visitService.createVisit(offenderNo, createVisitRequest)).isEqualTo(CreateVisitResponse(visitId))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitDate).isEqualTo(LocalDate.parse("2021-11-04"))
          assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T12:05"))
          assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
          assertThat(visit.visitType).isEqualTo(visitType)
          assertThat(visit.visitStatus.code).isEqualTo("SCH")
          assertThat(visit.location.id).isEqualTo(prisonId)
        }
      )
    }

    @Test
    fun `balance decrement is saved correctly when no privileged is available`() {

      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = offenderBookingId,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        }
      )
    }

    @Test
    fun `privilege balance decrement is saved correctly when available`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        }
      )
    }

    @Test
    fun `Visit order and balance adjustment is still created when balance is negative`() {

      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = -1,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = offenderBookingId,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(offenderNo, createVisitRequest)

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNotNull() })
      verify(offenderVisitBalanceAdjustmentRepository).save(any())
    }

    @Test
    fun `No visit order or balance adjustment is created when no balance record exists`() {

      defaultVisit.offenderBooking.visitBalance = null
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(offenderNo, createVisitRequest.copy(privileged = true))

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNull() })
      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }

    @Test
    fun `visitor records are saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

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
    fun `agency visit days are created if not found`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

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

      val thrown = assertThrows(NotFoundException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo(offenderNo)
    }

    @Test
    fun personNotFound() {
      whenever(personRepository.findById(45L)).thenReturn(Optional.empty())

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Person with id=45 does not exist")
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisit(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }
  }

  @DisplayName("create")
  @Nested
  internal inner class CreateVisitV2 {
    private val createVisitRequest = CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = prisonId,
      visitorPersonIds = listOf(45L, 46L),
      issueDate = LocalDate.parse("2021-11-02"),
    )

    private val defaultLocation = AgencyLocation(id = "SWI", description = "Swindon")

    private val defaultVisitDay = AgencyVisitDay(
      AgencyVisitDayId(
        location = defaultLocation,
        weekDay = "THU"
      )
    )

    private val defaultVisitTime =
      AgencyVisitTime(
        AgencyVisitTimeId(
          location = defaultLocation,
          timeSlotSequence = 10,
          weekDay = defaultVisitDay.agencyVisitDayId.weekDay
        ),
        startTime = LocalTime.parse("12:05"),
        endTime = LocalTime.parse("12:05"),
        effectiveDate = LocalDate.parse("2021-11-02"),
        expiryDate = LocalDate.parse("2021-11-02")
      )

    private val defaultInternalLocation = AgencyInternalLocation(
      locationId = 24,
      agencyId = defaultLocation.id,
      description = "SWI-VSIP",
      locationType = "VISIT",
      locationCode = "VSIP"
    )

    private val defaultVisitSlot = AgencyVisitSlot(
      id = 10,
      agencyVisitTime = defaultVisitTime,
      agencyInternalLocation = defaultInternalLocation,
      location = defaultLocation,
      weekDay = defaultVisitDay.agencyVisitDayId.weekDay,
      timeSlotSequence = 10,
    )

    @BeforeEach
    fun setupCreateV2() {
      whenever(visitDayRepository.save(any())).thenReturn(defaultVisitDay)
      whenever(visitTimeRepository.save(any())).thenReturn(defaultVisitTime)
      whenever(visitSlotRepository.save(any())).thenReturn(defaultVisitSlot)
      whenever(internalLocationRepository.save(any())).thenReturn(defaultInternalLocation)
    }

    @Test
    fun `visit data is mapped correctly`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      assertThat(visitService.createVisitV2(offenderNo, createVisitRequest)).isEqualTo(CreateVisitResponse(visitId))

      verify(visitDayRepository).save(any())
      verify(visitTimeRepository).save(any())
      verify(visitSlotRepository).save(any())
      verify(internalLocationRepository).save(any())
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitDate).isEqualTo(LocalDate.parse("2021-11-04"))
          assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T12:05"))
          assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
          assertThat(visit.visitType).isEqualTo(visitType)
          assertThat(visit.visitStatus.code).isEqualTo("SCH")
          assertThat(visit.location.id).isEqualTo(prisonId)
          assertThat(visit.agencyInternalLocation?.description).isEqualTo("SWI-VSIP")
          assertThat(visit.agencyInternalLocation?.locationCode).isEqualTo("VSIP")
          assertThat(visit.agencyInternalLocation?.locationType).isEqualTo("VISIT")
          assertThat(visit.agencyVisitSlot!!.agencyVisitTime.startTime).isEqualTo(LocalTime.parse("12:05"))
        }
      )
    }

    @Test
    fun `balance decrement is saved correctly when no privileged is available`() {

      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = offenderBookingId,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisitV2(offenderNo, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        }
      )
    }

    @Test
    fun `privilege balance decrement is saved correctly when available`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisitV2(offenderNo, createVisitRequest.copy(privileged = true))

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        }
      )
    }

    @Test
    fun `Visit order and balance adjustment is still created when balance is negative`() {

      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = -1,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = offenderBookingId,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisitV2(offenderNo, createVisitRequest)

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNotNull() })
      verify(offenderVisitBalanceAdjustmentRepository).save(any())
    }

    @Test
    fun `No visit order or balance adjustment is created when no balance record exists`() {

      defaultVisit.offenderBooking.visitBalance = null
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisitV2(offenderNo, createVisitRequest.copy(privileged = true))

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNull() })
      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }

    @Test
    fun `visitor records are saved correctly`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisitV2(offenderNo, createVisitRequest)

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
    fun `existing agency visit Days are used - if match exists`() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(visitDayRepository.findById(AgencyVisitDayId(defaultLocation, "THU"))).thenReturn(
        Optional.of(
          defaultVisitDay
        )
      )

      visitService.createVisitV2(offenderNo, createVisitRequest)

      verify(visitDayRepository, never()).save(any())
    }

    @Test
    fun `existing agency visit Times are used - if match exists `() {

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(visitDayRepository.findById(AgencyVisitDayId(defaultLocation, "THU"))).thenReturn(
        Optional.of(
          defaultVisitDay
        )
      )
      whenever(
        visitTimeRepository.findByStartTimeAndAgencyVisitTimesId_WeekDayAndAgencyVisitTimesId_Location(
          startTime = defaultVisitTime.startTime,
          weekDay = defaultVisitTime.agencyVisitTimesId.weekDay,
          location = defaultVisitTime.agencyVisitTimesId.location
        )
      ).thenReturn(defaultVisitTime)

      whenever(
        visitTimeRepository.findFirstByAgencyVisitTimesId_LocationAndAgencyVisitTimesId_WeekDayOrderByAgencyVisitTimesId_TimeSlotSequenceDesc(
          agencyId = defaultLocation,
          "THU"
        )
      ).thenReturn(defaultVisitTime)

      visitService.createVisitV2(offenderNo, createVisitRequest)

      verify(visitTimeRepository, never()).save(any())
    }

    @Test
    fun `existing agency visit slots are used - if match exists `() {

      whenever(
        visitTimeRepository.findByStartTimeAndAgencyVisitTimesId_WeekDayAndAgencyVisitTimesId_Location(
          startTime = defaultVisitTime.startTime,
          weekDay = defaultVisitTime.agencyVisitTimesId.weekDay,
          location = defaultVisitTime.agencyVisitTimesId.location
        )
      ).thenReturn(defaultVisitTime)

      whenever(
        visitTimeRepository.findFirstByAgencyVisitTimesId_LocationAndAgencyVisitTimesId_WeekDayOrderByAgencyVisitTimesId_TimeSlotSequenceDesc(
          agencyId = defaultLocation,
          "THU"
        )
      ).thenReturn(defaultVisitTime)

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(visitDayRepository.findById(AgencyVisitDayId(defaultLocation, defaultVisitDay.agencyVisitDayId.weekDay))).thenReturn(
        Optional.of(
          defaultVisitDay
        )
      )
      whenever(
        visitSlotRepository.findByAgencyInternalLocation_DescriptionAndAgencyVisitTime_StartTimeAndWeekDay(
          roomDescription = defaultInternalLocation.description,
          startTime = createVisitRequest.startDateTime.toLocalTime(),
          weekDay = VisitService.Companion.dayOfWeekNomisMap[createVisitRequest.startDateTime.dayOfWeek.value]!!
        )
      ).thenReturn(defaultVisitSlot)

      visitService.createVisitV2(offenderNo, createVisitRequest)

      verify(visitSlotRepository, never()).save(any())
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)).thenReturn(
        Optional.empty()
      )

      val thrown = assertThrows(NotFoundException::class.java) {
        visitService.createVisitV2(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo(offenderNo)
    }

    @Test
    fun personNotFound() {
      whenever(personRepository.findById(45L)).thenReturn(Optional.empty())
      whenever(visitDayRepository.save(any())).thenReturn(defaultVisitDay)
      whenever(visitTimeRepository.save(any())).thenReturn(defaultVisitTime)
      whenever(visitSlotRepository.save(any())).thenReturn(defaultVisitSlot)
      whenever(internalLocationRepository.save(any())).thenReturn(defaultInternalLocation)

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisitV2(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Person with id=45 does not exist")
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(prisonId)).thenReturn(Optional.empty())

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisitV2(offenderNo, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$prisonId does not exist")
    }
  }

  @DisplayName("cancel")
  @Nested
  internal inner class CancelVisit {
    private val cancelVisitRequest = CancelVisitRequest(outcome = "OFFCANC")

    @Test
    fun `visit data is amended correctly`() {

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(defaultVisit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      with(defaultVisit) {
        assertThat(visitStatus.code).isEqualTo("CANC")

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

      defaultVisit.visitOrder?.visitOrderType = VisitOrderType("VO", "desc")

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(defaultVisit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode?.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_CANCEL)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Booking cancelled by VSIP")
        }
      )
    }

    @Test
    fun `privilege balance increment is saved correctly`() {

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(defaultVisit))

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

    @Test
    fun `No balance exists`() {

      defaultVisit.offenderBooking.visitBalance = null

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(defaultVisit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }

    @Test
    fun `No visit order exists`() {

      defaultVisit.visitOrder = null

      whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(defaultVisit))

      visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }
  }

  @DisplayName("get")
  @Nested
  internal inner class GetVisit {
    @Test
    fun `visit data is mapped correctly`() {

      whenever(visitRepository.findById(123)).thenReturn(Optional.of(defaultVisit))

      val visitResponse = visitService.getVisit(123)
      assertThat(visitResponse.startDateTime).isEqualTo(defaultVisit.startDateTime)
      assertThat(visitResponse.endDateTime).isEqualTo(defaultVisit.endDateTime)
      assertThat(visitResponse.visitType.code).isEqualTo(defaultVisit.visitType.code)
      assertThat(visitResponse.visitType.description).isEqualTo(defaultVisit.visitType.description)
      assertThat(visitResponse.visitStatus.code).isEqualTo(defaultVisit.visitStatus.code)
      assertThat(visitResponse.visitStatus.description).isEqualTo(defaultVisit.visitStatus.description)
      assertThat(visitResponse.prisonId).isEqualTo(defaultVisit.location.id)
      assertThat(visitResponse.offenderNo).isEqualTo(defaultVisit.offenderBooking.offender.nomsId)
      assertThat(visitResponse.commentText).isEqualTo(defaultVisit.commentText)
      assertThat(visitResponse.visitorConcernText).isEqualTo(defaultVisit.visitorConcernText)
    }

    @Test
    fun `visit ids by filter data are mapped correctly`() {

      val visitFilter = VisitFilter(
        prisonIds = listOf(),
        visitTypes = listOf(),
        toDateTime = LocalDateTime.now(),
        fromDateTime = LocalDateTime.now(),
        ignoreMissingRoom = false
      )
      val pageRequest = PageRequest.of(0, 1)

      whenever(visitRepository.findAll(any<VisitSpecification>(), any<PageRequest>())).thenReturn(
        PageImpl(listOf(defaultVisit))
      )
      val visitList = visitService.findVisitIdsByFilter(
        pageRequest,
        visitFilter
      )
      assertThat(visitList).extracting("visitId").containsExactly(defaultVisit.id)
    }
  }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.ServiceAgencySwitchesService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderVisitorRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.VisitSpecification
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

private const val OFFENDER_BOOKING_ID = -9L
private const val VISIT_ID = 1008L
private const val OFFENDER_NO = "A1234AA"
private const val PRISON_ID = "SWI"
private const val EVENT_ID = 34L
private const val VISIT_ORDER = 54L

internal class VisitServiceTest {

  private val visitRepository: VisitRepository = mock()
  private val visitVisitorRepository: VisitVisitorRepository = mock()
  private val visitOrderRepository: VisitOrderRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val personRepository: PersonRepository = mock()
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository = mock()
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository = mock()
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
  private val visitOrderVisitorRepository: VisitOrderVisitorRepository = mock()
  private val serviceAgencySwitchesService: ServiceAgencySwitchesService = mock()

  private val visitService: VisitService = VisitService(
    visitRepository,
    visitVisitorRepository,
    visitOrderRepository,
    offenderBookingRepository,
    offenderVisitBalanceAdjustmentRepository,
    offenderVisitBalanceRepository,
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
    internalLocationRepository,
    visitOrderVisitorRepository,
    serviceAgencySwitchesService,
  )

  val visitType = VisitType("SCON", "desc")
  val defaultOffender = Offender(nomsId = OFFENDER_NO, lastName = "Smith", firstName = "John", gender = Gender("MALE", "Male"))
  private val defaultOffenderBooking = OffenderBooking(
    bookingId = OFFENDER_BOOKING_ID,
    offender = defaultOffender,
    bookingBeginDate = LocalDateTime.now(),
    location = AgencyLocation("MKI", "Millsike Prison"),
  ).apply {
    // add circular reference
    visitBalance = OffenderVisitBalance(
      offenderBooking = this,
      remainingVisitOrders = 3,
      remainingPrivilegedVisitOrders = 5,
    )
  }
  val defaultVisit = Visit(
    id = VISIT_ID,
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
      code = "SCON",
      description = "Social contact",
    ),
    commentText = "some comments",
    visitorConcernText = "concerns",
  ).apply {
    // add circular reference
    visitors.add(VisitVisitor(visit = this, offenderBooking = defaultOffenderBooking))
    visitors.add(VisitVisitor(visit = this, person = Person(-7L, "First", "Last")))
  }

  @BeforeEach
  fun setup() {
    whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(OFFENDER_NO, true)).thenReturn(
      Optional.of(defaultOffenderBooking),
    )
    whenever(personRepository.findById(any())).thenAnswer {
      Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There"))
    }
    whenever(visitTypeRepository.findById(VisitType.pk("SCON"))).thenReturn(Optional.of(visitType))
    whenever(visitStatusRepository.findById(any())).thenAnswer {
      Optional.of(VisitStatus((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(eventStatusRepository.findById(any())).thenAnswer {
      Optional.of(EventStatus((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(visitOrderAdjustmentReasonRepository.findById(any())).thenAnswer {
      Optional.of(VisitOrderAdjustmentReason((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(visitOrderTypeRepository.findById(any())).thenAnswer {
      Optional.of(VisitOrderType((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(visitOutcomeRepository.findById(any())).thenAnswer {
      Optional.of(VisitOutcomeReason((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(eventOutcomeRepository.findById(any())).thenAnswer {
      Optional.of(EventOutcome((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(agencyLocationRepository.findById(any())).thenAnswer {
      Optional.of(AgencyLocation(it.arguments[0] as String, "desc"))
    }

    whenever(visitVisitorRepository.getEventId()).thenReturn(EVENT_ID)
    whenever(visitOrderRepository.getVisitOrderNumber()).thenReturn(VISIT_ORDER)

    whenever(visitDayRepository.save(any())).thenAnswer { it.arguments[0] as AgencyVisitDay }
    whenever(visitTimeRepository.save(any())).thenAnswer { it.arguments[0] as AgencyVisitTime }
    whenever(visitSlotRepository.save(any())).thenAnswer { it.arguments[0] as AgencyVisitSlot }
    whenever(internalLocationRepository.save(any())).thenAnswer { it.arguments[0] as AgencyInternalLocation }
    whenever(visitRepository.save(any())).thenAnswer { (it.arguments[0] as Visit).copy(id = VISIT_ID) }
    whenever(
      internalLocationRepository.findByAgencyIdAndActiveAndLocationCodeInAndParentLocationIsNull(
        eq(PRISON_ID),
        any(),
        any(),
      ),
    ).thenAnswer {
      AgencyInternalLocation(
        locationId = 99,
        active = true,
        locationType = "AREA",
        agency = AgencyLocation(it.arguments[0] as String, "desc"),
        description = "${it.arguments[0]}-VISITS",
        locationCode = "VISITS",
        userDescription = "VISITS",
      )
    }
  }

  @DisplayName("create")
  @Nested
  internal inner class CreateVisit {
    private val createVisitRequest = CreateVisitRequest(
      visitType = "SCON",
      startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
      endTime = LocalTime.parse("13:04"),
      prisonId = PRISON_ID,
      visitorPersonIds = listOf(45L, 46L),
      issueDate = LocalDate.parse("2021-11-02"),
      room = "Main visit room",
      openClosedStatus = "OPEN",
    )

    @Test
    fun `visit data is mapped correctly`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest)).isEqualTo(CreateVisitResponse(VISIT_ID))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitDate).isEqualTo(LocalDate.parse("2021-11-04"))
          assertThat(visit.startDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T12:05"))
          assertThat(visit.endDateTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
          assertThat(visit.visitType).isEqualTo(visitType)
          assertThat(visit.visitStatus.code).isEqualTo("SCH")
          assertThat(visit.location.id).isEqualTo(PRISON_ID)
        },
      )
    }

    @Test
    fun `visit data room details are mapped correctly for social visits`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest)).isEqualTo(CreateVisitResponse(VISIT_ID))

      verify(visitDayRepository).save(any())
      verify(visitTimeRepository).save(any())
      verify(visitSlotRepository).save(any())
      verify(internalLocationRepository).save(any())
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.location.id).isEqualTo(PRISON_ID)
          assertThat(visit.agencyInternalLocation?.locationType).isEqualTo("VISIT")
          assertThat(visit.agencyInternalLocation?.locationCode).isEqualTo("VSIP_SOC")
          assertThat(visit.agencyInternalLocation?.active).isTrue()
          assertThat(visit.agencyInternalLocation?.userDescription).isEqualTo("VISITS - SOCIAL")
          assertThat(visit.agencyInternalLocation?.parentLocation?.locationCode).isEqualTo("VISITS")
          assertThat(visit.agencyVisitSlot!!.agencyVisitTime.startTime).isEqualTo(LocalTime.parse("12:05"))
        },
      )
    }

    @Test
    fun `visit data room details are mapped correctly for closed visits`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(openClosedStatus = "CLOSED"))).isEqualTo(CreateVisitResponse(VISIT_ID))

      verify(visitDayRepository).save(any())
      verify(visitTimeRepository).save(any())
      verify(visitSlotRepository).save(any())
      verify(internalLocationRepository).save(any())
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.location.id).isEqualTo(PRISON_ID)
          assertThat(visit.agencyInternalLocation?.locationType).isEqualTo("VISIT")
          assertThat(visit.agencyInternalLocation?.locationCode).isEqualTo("VSIP_CLO")
          assertThat(visit.agencyInternalLocation?.active).isTrue()
          assertThat(visit.agencyInternalLocation?.userDescription).isEqualTo("VISITS - CLOSED")
          assertThat(visit.agencyInternalLocation?.parentLocation?.locationCode).isEqualTo("VISITS")
          assertThat(visit.agencyVisitSlot!!.agencyVisitTime.startTime).isEqualTo(LocalTime.parse("12:05"))
        },
      )
    }

    @Test
    fun `visit room will have a parent location when present`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest)).isEqualTo(CreateVisitResponse(VISIT_ID))
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.agencyInternalLocation?.description).isEqualTo("$PRISON_ID-VISITS-VSIP_SOC")
          assertThat(visit.agencyInternalLocation?.parentLocation).isNotNull()
        },
      )
    }

    @Test
    fun `visit room will not have a parent location when present`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(prisonId = "WWI"))).isEqualTo(CreateVisitResponse(VISIT_ID))
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.agencyInternalLocation?.description).isEqualTo("WWI-VISITS-VSIP_SOC")
          assertThat(visit.agencyInternalLocation?.parentLocation).isNull()
        },
      )
    }

    @Test
    fun `visit data room details are always the same regardless of VSIP room`() {
      assertThat(visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(room = "This is a really really extremely long name"))).isEqualTo(CreateVisitResponse(VISIT_ID))
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.agencyInternalLocation?.userDescription).isEqualTo("VISITS - SOCIAL")
        },
      )
    }

    @Test
    internal fun `room description is not used for NOMIS room description`() {
      visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(room = "Main visit room", openClosedStatus = "OPEN"))
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.agencyInternalLocation?.description).isEqualTo("SWI-VISITS-VSIP_SOC")
          assertThat(visit.agencyInternalLocation?.locationCode).isEqualTo("VSIP_SOC")
        },
      )
    }

    @Test
    internal fun `visit restriction is used for NOMIS room description`() {
      visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(room = "Main visit room", openClosedStatus = "CLOSED"))
      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.agencyInternalLocation?.description).isEqualTo("SWI-VISITS-VSIP_CLO")
          assertThat(visit.agencyInternalLocation?.locationCode).isEqualTo("VSIP_CLO")
        },
      )
    }

    @Test
    fun `balance decrement is saved correctly when no privileged is available`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        },
      )
    }

    @Test
    fun `privilege balance decrement is saved correctly when available`() {
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_ISSUE)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(-1)
          assertThat(balanceArgument.commentText).isEqualTo("Created by VSIP")
        },
      )
    }

    @Test
    fun `balance decrement is not saved if DPS in charge of allocation`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(true)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verifyNoInteractions(offenderVisitBalanceAdjustmentRepository)
      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNotNull() })
    }

    @Test
    fun `privilege balance decrement is not saved if DPS in charge of allocation`() {
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(true)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verifyNoInteractions(offenderVisitBalanceAdjustmentRepository)
      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNotNull() })
    }

    @Test
    fun `Visit order and balance adjustment is still created when balance is negative`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = -1,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNotNull() })
      verify(offenderVisitBalanceAdjustmentRepository).save(any())
    }

    @Test
    fun `No visit order or balance adjustment is created when no balance record exists`() {
      defaultVisit.offenderBooking.visitBalance = null
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(visitRepository).save(check { visit -> assertThat(visit.visitOrder).isNull() })
      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }

    @Test
    fun `visitor records are saved correctly`() {
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitors).extracting("offenderBooking.bookingId", "person.id", "eventStatus.code", "eventId")
            .containsExactly(
              Tuple.tuple(OFFENDER_BOOKING_ID, null, "SCH", EVENT_ID),
              Tuple.tuple(null, 45L, "SCH", null),
              Tuple.tuple(null, 46L, "SCH", null),
            )
        },
      )
    }

    @Test
    fun `agency visit days are created if not found`() {
      whenever(visitRepository.save(any())).thenReturn(defaultVisit)

      visitService.createVisit(OFFENDER_NO, createVisitRequest)

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitors).extracting("offenderBooking.bookingId", "person.id", "eventStatus.code", "eventId")
            .containsExactly(
              Tuple.tuple(OFFENDER_BOOKING_ID, null, "SCH", EVENT_ID),
              Tuple.tuple(null, 45L, "SCH", null),
              Tuple.tuple(null, 46L, "SCH", null),
            )
        },
      )
    }

    @Test
    fun offenderNotFound() {
      whenever(offenderBookingRepository.findByOffenderNomsIdAndActive(OFFENDER_NO, true)).thenReturn(
        Optional.empty(),
      )

      val thrown = assertThrows(NotFoundException::class.java) {
        visitService.createVisit(OFFENDER_NO, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo(OFFENDER_NO)
    }

    @Test
    fun personNotFound() {
      whenever(personRepository.findById(45L)).thenReturn(Optional.empty())

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisit(OFFENDER_NO, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Person with id=45 does not exist")
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows(BadDataException::class.java) {
        visitService.createVisit(OFFENDER_NO, createVisitRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$PRISON_ID does not exist")
    }

    @Test
    fun `lead visitor set to first person over 18`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)
      whenever(personRepository.findById(45)).thenAnswer {
        // child
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now()))
      }
      whenever(personRepository.findById(46)).thenAnswer {
        // 18 year old
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now().minusYears(18)))
      }
      whenever(personRepository.findById(47)).thenAnswer {
        // someone without birth date
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = null))
      }

      visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(visitorPersonIds = listOf(45, 46, 47)))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitOrder?.visitors?.map { it.groupLeader }).containsExactly(false, true, false)
        },
      )
    }

    @Test
    fun `lead visitor set to first person without date of birth`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)
      whenever(personRepository.findById(45)).thenAnswer {
        // child
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now()))
      }
      whenever(personRepository.findById(46)).thenAnswer {
        // 16 year old
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now().minusYears(16)))
      }
      whenever(personRepository.findById(47)).thenAnswer {
        // someone without birth date
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = null))
      }

      visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(visitorPersonIds = listOf(45, 46, 47)))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitOrder?.visitors?.map { it.groupLeader }).containsExactly(false, false, true)
        },
      )
    }

    @Test
    fun `lead visitor set to first visitor if all under 18`() {
      defaultVisit.offenderBooking.visitBalance =
        OffenderVisitBalance(
          remainingVisitOrders = 3,
          remainingPrivilegedVisitOrders = 0,
          offenderBooking = OffenderBooking(
            bookingId = OFFENDER_BOOKING_ID,
            bookingBeginDate = LocalDateTime.now(),
            offender = defaultOffender,
          ),
        )

      whenever(visitRepository.save(any())).thenReturn(defaultVisit)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)
      whenever(personRepository.findById(45)).thenAnswer {
        // child
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now()))
      }
      whenever(personRepository.findById(46)).thenAnswer {
        // 16 year old
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now().minusYears(16)))
      }
      whenever(personRepository.findById(47)).thenAnswer {
        // 18 year old minus a day
        Optional.of(Person(id = it.arguments[0] as Long, firstName = "Hi", lastName = "There", birthDate = LocalDate.now().minusYears(18).plusDays(1)))
      }

      visitService.createVisit(OFFENDER_NO, createVisitRequest.copy(visitorPersonIds = listOf(45, 46, 47)))

      verify(visitRepository).save(
        check { visit ->
          assertThat(visit.visitOrder?.visitors?.map { it.groupLeader }).containsExactly(true, false, false)
        },
      )
    }
  }

  @DisplayName("cancel")
  @Nested
  internal inner class CancelVisit {
    private val cancelVisitRequest = CancelVisitRequest(outcome = "OFFCANC")

    @Test
    fun `visit data is amended correctly`() {
      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      with(defaultVisit) {
        assertThat(visitStatus.code).isEqualTo("CANC")

        assertThat(visitors).extracting("eventOutcome.code", "eventStatus.code", "outcomeReasonCode")
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

      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)
      whenever(offenderVisitBalanceRepository.findByIdForUpdate(OFFENDER_BOOKING_ID)).thenReturn(defaultOffenderBooking.visitBalance)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode.code).isEqualTo(VisitOrderAdjustmentReason.VISIT_ORDER_CANCEL)
          assertThat(balanceArgument.remainingVisitOrders).isEqualTo(1)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Booking cancelled by VSIP")
        },
      )
      verify(serviceAgencySwitchesService).checkServiceAgency("VISIT_ALLOCATION", "MKI")
    }

    @Test
    fun `balance increment is not saved if DPS in charge of allocation`() {
      defaultVisit.visitOrder?.visitOrderType = VisitOrderType("VO", "desc")

      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)
      whenever(offenderVisitBalanceRepository.findByIdForUpdate(OFFENDER_BOOKING_ID)).thenReturn(defaultOffenderBooking.visitBalance)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(true)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      verifyNoInteractions(offenderVisitBalanceAdjustmentRepository)
      verify(serviceAgencySwitchesService).checkServiceAgency("VISIT_ALLOCATION", "MKI")
    }

    @Test
    fun `privilege balance increment is saved correctly`() {
      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)
      whenever(offenderVisitBalanceRepository.findByIdForUpdate(OFFENDER_BOOKING_ID)).thenReturn(defaultOffenderBooking.visitBalance)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(false)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository).save(
        check { balanceArgument ->
          assertThat(balanceArgument.adjustReasonCode.code).isEqualTo(VisitOrderAdjustmentReason.PRIVILEGED_VISIT_ORDER_CANCEL)
          assertThat(balanceArgument.remainingPrivilegedVisitOrders).isEqualTo(1)
          assertThat(balanceArgument.remainingVisitOrders).isNull()
          assertThat(balanceArgument.commentText).isEqualTo("Booking cancelled by VSIP")
        },
      )
    }

    @Test
    fun `privilege balance increment is not saved if DPS in charge of balance`() {
      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)
      whenever(offenderVisitBalanceRepository.findByIdForUpdate(OFFENDER_BOOKING_ID)).thenReturn(defaultOffenderBooking.visitBalance)
      whenever(serviceAgencySwitchesService.checkServiceAgency(any(), any())).thenReturn(true)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      verifyNoInteractions(offenderVisitBalanceAdjustmentRepository)
      verify(serviceAgencySwitchesService).checkServiceAgency("VISIT_ALLOCATION", "MKI")
    }

    @Test
    fun `No balance exists`() {
      defaultVisit.offenderBooking.visitBalance = null

      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

      verify(offenderVisitBalanceAdjustmentRepository, never()).save(any())
    }

    @Test
    fun `No visit order exists`() {
      defaultVisit.visitOrder = null

      whenever(visitRepository.findByIdForUpdate(VISIT_ID)).thenReturn(defaultVisit)

      visitService.cancelVisit(OFFENDER_NO, VISIT_ID, cancelVisitRequest)

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
      )
      val pageRequest = PageRequest.of(0, 1)

      whenever(visitRepository.findAll(any<VisitSpecification>(), any<PageRequest>())).thenReturn(
        PageImpl(listOf(defaultVisit)),
      )
      val visitList = visitService.findVisitIdsByFilter(
        pageRequest,
        visitFilter,
      )
      assertThat(visitList).extracting("visitId").containsExactly(defaultVisit.id)
    }
  }
}

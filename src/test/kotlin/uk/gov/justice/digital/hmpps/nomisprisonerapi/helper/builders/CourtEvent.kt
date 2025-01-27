package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CourtEventDslMarker

@NomisDataDslMarker
interface CourtEventDsl {
  @CourtEventChargeDslMarker
  fun courtEventCharge(
    offenderCharge: OffenderCharge,
    offencesCount: Int? = null,
    offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 5),
    plea: String? = "G",
    propertyValue: BigDecimal? = null,
    totalPropertyValue: BigDecimal? = null,
    cjitCode1: String? = "cj1",
    cjitCode2: String? = "cj2",
    cjitCode3: String? = "cj3",
    resultCode1: String? = "1002",
    resultCode2: String? = "1003",
    resultCode1Indicator: String? = "P",
    resultCode2Indicator: String? = "P",
    mostSeriousFlag: Boolean = false,
    whenModified: LocalDateTime? = null,
    dsl: CourtEventChargeDsl.() -> Unit = {},
  ): CourtEventCharge

  @CourtOrderDslMarker
  fun courtOrder(
    courtDate: LocalDate = LocalDate.of(2023, 1, 1),
    issuingCourt: String = "MDI",
    orderType: String = "AUTO",
    orderStatus: String = "A",
    requestDate: LocalDate? = LocalDate.of(2023, 1, 1),
    dueDate: LocalDate? = LocalDate.of(2023, 1, 5),
    courtInfoId: String? = "A12345",
    seriousnessLevel: String? = "HIGH",
    commentText: String? = "a court order comment",
    nonReportFlag: Boolean = false,
    dsl: CourtOrderDsl.() -> Unit = {},
  ): CourtOrder
}

@Component
class CourtEventBuilderFactory(
  private val repository: CourtEventBuilderRepository,
  private val courtEventChargeBuilderFactory: CourtEventChargeBuilderFactory,
  private val courtOrderBuilderFactory: CourtOrderBuilderFactory,
) {
  fun builder(): CourtEventBuilder = CourtEventBuilder(
    repository,
    courtEventChargeBuilderFactory,
    courtOrderBuilderFactory,
  )
}

@Component
class CourtEventBuilderRepository(
  val repository: CourtEventRepository,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val directionTypeRepository: ReferenceCodeRepository<DirectionType>,
  val courtEventTypeRepository: ReferenceCodeRepository<MovementReason>,
  val agencyLocationRepository: AgencyLocationRepository,
  val offenceResultCodeRepository: OffenceResultCodeRepository,
) {
  fun save(courtEvent: CourtEvent): CourtEvent = repository.save(courtEvent)

  fun lookupEventStatus(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!

  fun lookupOffenceResultCode(code: String): OffenceResultCode = offenceResultCodeRepository.findByIdOrNull(code)!!

  fun lookupDirectionType(code: String): DirectionType = directionTypeRepository.findByIdOrNull(DirectionType.pk(code))!!

  fun lookupCourtEventType(code: String): MovementReason = courtEventTypeRepository.findByIdOrNull(MovementReason.pk(code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

class CourtEventBuilder(
  private val repository: CourtEventBuilderRepository,
  private val courtEventChargeBuilderFactory: CourtEventChargeBuilderFactory,
  private val courtOrderBuilderFactory: CourtOrderBuilderFactory,
) : CourtEventDsl {
  private lateinit var courtEvent: CourtEvent

  fun build(
    commentText: String?,
    prison: String,
    courtEventType: String,
    eventStatusCode: String,
    outcomeReasonCode: String?,
    judgeName: String?,
    eventDateTime: LocalDateTime,
    nextEventDateTime: LocalDateTime?,
    offenderBooking: OffenderBooking,
    courtCase: CourtCase?,
    orderRequestedFlag: Boolean?,
  ): CourtEvent = CourtEvent(
    offenderBooking = offenderBooking,
    courtCase = courtCase,
    eventDate = eventDateTime.toLocalDate(),
    startTime = eventDateTime,
    courtEventType = repository.lookupCourtEventType(courtEventType),
    judgeName = judgeName,
    eventStatus = repository.lookupEventStatus(eventStatusCode),
    court = repository.lookupAgency(prison),
    outcomeReasonCode = outcomeReasonCode?.let { repository.lookupOffenceResultCode(outcomeReasonCode) },
    commentText = commentText,
    orderRequestedFlag = orderRequestedFlag,
    nextEventStartTime = nextEventDateTime,
    nextEventDate = nextEventDateTime?.toLocalDate(),
    directionCode = repository.lookupDirectionType(DirectionType.OUT),
  )
    .let { repository.save(it) }
    .also { courtEvent = it }

  override fun courtEventCharge(
    offenderCharge: OffenderCharge,
    offencesCount: Int?,
    offenceDate: LocalDate?,
    offenceEndDate: LocalDate?,
    plea: String?,
    propertyValue: BigDecimal?,
    totalPropertyValue: BigDecimal?,
    cjitCode1: String?,
    cjitCode2: String?,
    cjitCode3: String?,
    resultCode1: String?,
    resultCode2: String?,
    resultCode1Indicator: String?,
    resultCode2Indicator: String?,
    mostSeriousFlag: Boolean,
    whenModified: LocalDateTime?,
    dsl: CourtEventChargeDsl.() -> Unit,
  ) = courtEventChargeBuilderFactory.builder().let { builder ->
    builder.build(
      courtEvent = courtEvent,
      offenderCharge = offenderCharge,
      offencesCount = offencesCount,
      offenceDate = offenceDate,
      offenceEndDate = offenceEndDate,
      plea = plea,
      propertyValue = propertyValue,
      totalPropertyValue = totalPropertyValue,
      cjitCode1 = cjitCode1,
      cjitCode2 = cjitCode2,
      cjitCode3 = cjitCode3,
      resultCode1 = resultCode1,
      resultCode2 = resultCode2,
      resultCode1Indicator = resultCode1Indicator,
      resultCode2Indicator = resultCode2Indicator,
      mostSeriousFlag = mostSeriousFlag,
      whenModified = whenModified,
    )
      .also { courtEvent.courtEventCharges += it }
      .also { builder.apply(dsl) }
  }

  override fun courtOrder(
    courtDate: LocalDate,
    issuingCourt: String,
    orderType: String,
    orderStatus: String,
    requestDate: LocalDate?,
    dueDate: LocalDate?,
    courtInfoId: String?,
    seriousnessLevel: String?,
    commentText: String?,
    nonReportFlag: Boolean,
    dsl: CourtOrderDsl.() -> Unit,
  ) = courtOrderBuilderFactory.builder().let { builder ->
    builder.build(
      courtEvent = courtEvent,
      courtDate = courtDate,
      issuingCourt = issuingCourt,
      orderType = orderType,
      orderStatus = orderStatus,
      requestDate = requestDate,
      dueDate = dueDate,
      courtInfoId = courtInfoId,
      seriousnessLevel = seriousnessLevel,
      commentText = commentText,
      nonReportFlag = nonReportFlag,
    )
      .also { courtEvent.courtOrders += it }
      .also { builder.apply(dsl) }
  }
}

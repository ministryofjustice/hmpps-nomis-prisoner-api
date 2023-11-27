package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
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
    offencesCount: Int? = 1,
    offenceDate: LocalDate? = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate? = LocalDate.of(2023, 1, 5),
    plea: String? = "G",
    propertyValue: BigDecimal? = BigDecimal(3.2),
    totalPropertyValue: BigDecimal? = BigDecimal(10),
    cjitCode1: String? = "cj1",
    cjitCode2: String? = "cj2",
    cjitCode3: String? = "cj3",
    resultCode1: String? = "1002",
    resultCode2: String? = "1003",
    resultCode1Indicator: String? = "rci1",
    resultCode2Indicator: String? = "rci2",
    mostSeriousFlag: Boolean = false,
    dsl: CourtEventChargeDsl.() -> Unit = {},
  ): CourtEventCharge
}

@Component
class CourtEventBuilderFactory(
  private val repository: CourtEventBuilderRepository,
  private val courtEventChargeBuilderFactory: CourtEventChargeBuilderFactory,
) {
  fun builder(): CourtEventBuilder {
    return CourtEventBuilder(
      repository,
      courtEventChargeBuilderFactory,
    )
  }
}

@Component
class CourtEventBuilderRepository(
  val repository: CourtEventRepository,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val directionTypeRepository: ReferenceCodeRepository<DirectionType>,
  val courtEventTypeRepository: ReferenceCodeRepository<CourtEventType>,
  val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(courtEvent: CourtEvent): CourtEvent =
    repository.save(courtEvent)

  fun lookupEventStatus(code: String): EventStatus =
    eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!

  fun lookupDirectionType(code: String): DirectionType =
    directionTypeRepository.findByIdOrNull(DirectionType.pk(code))!!

  fun lookupCourtEventType(code: String): CourtEventType =
    courtEventTypeRepository.findByIdOrNull(CourtEventType.pk(code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

class CourtEventBuilder(
  private val repository: CourtEventBuilderRepository,
  private val courtEventChargeBuilderFactory: CourtEventChargeBuilderFactory,
) : CourtEventDsl {
  private lateinit var courtEvent: CourtEvent

  fun build(
    commentText: String?,
    prison: String,
    courtEventType: String,
    eventStatusCode: String,
    outcomeReasonCode: String?,
    judgeName: String?,
    directionCode: String?,
    eventDate: LocalDate,
    startTime: LocalDateTime,
    nextEventStartTime: LocalDateTime?,
    nextEventDate: LocalDate?,
    offenderBooking: OffenderBooking,
    courtCase: CourtCase?,
    nextEventRequestFlag: Boolean?,
    orderRequestedFlag: Boolean?,
    holdFlag: Boolean?,
  ): CourtEvent = CourtEvent(
    offenderBooking = offenderBooking,
    courtCase = courtCase,
    eventDate = eventDate,
    startTime = startTime,
    courtEventType = repository.lookupCourtEventType(courtEventType),
    judgeName = judgeName,
    eventStatus = repository.lookupEventStatus(eventStatusCode),
    prison = repository.lookupAgency(prison),
    outcomeReasonCode = outcomeReasonCode,
    commentText = commentText,
    nextEventRequestFlag = nextEventRequestFlag,
    orderRequestedFlag = orderRequestedFlag,
    holdFlag = holdFlag,
    nextEventStartTime = nextEventStartTime,
    nextEventDate = nextEventDate,
    directionCode = directionCode?.let { repository.lookupDirectionType(directionCode) },
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
    dsl: CourtEventChargeDsl.() -> Unit,
  ) =
    courtEventChargeBuilderFactory.builder().let { builder ->
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
      )
        .also { courtEvent.courtEventCharges += it }
        .also { builder.apply(dsl) }
    }
}

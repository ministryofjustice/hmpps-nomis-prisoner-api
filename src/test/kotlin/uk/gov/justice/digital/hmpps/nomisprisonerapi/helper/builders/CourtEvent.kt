package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CourtEventDslMarker

@NomisDataDslMarker
interface CourtEventDsl

@Component
class CourtEventBuilderFactory(
  private val repository: CourtEventBuilderRepository,
) {
  fun builder(): CourtEventBuilder {
    return CourtEventBuilder(
      repository,
    )
  }
}

@Component
class CourtEventBuilderRepository(
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val directionTypeRepository: ReferenceCodeRepository<DirectionType>,
  val courtEventTypeRepository: ReferenceCodeRepository<CourtEventType>,
  val agencyLocationRepository: AgencyLocationRepository,
) {
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
    directionCode = directionCode?. let { repository.lookupDirectionType(directionCode) },
  )
    .also { courtEvent = it }
}

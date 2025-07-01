package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTapIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTapOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderScheduledTapOutDslMarker

@NomisDataDslMarker
interface OffenderScheduledTapOutDsl {
  @OffenderScheduledTapInDslMarker
  fun scheduledTapIn(
    eventDate: LocalDate = LocalDate.now().plusDays(1),
    startTime: LocalDateTime = LocalDateTime.now().plusDays(1),
    eventSubType: String = "R25",
    eventStatus: String = "SCH",
    comment: String? = "Tapped IN",
    escort: String = "U",
    toPrison: String = "LEI",
  ): OffenderScheduledTapIn
}

@Component
class OffenderScheduledTapOutBuilderRepository(
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val tapTransportTypeRepository: ReferenceCodeRepository<TapTransportType>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun eventStatusOf(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun tapTransportTypeOf(code: String): TapTransportType = tapTransportTypeRepository.findByIdOrNull(TapTransportType.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderScheduledTapOutBuilderFactory(
  private val repository: OffenderScheduledTapOutBuilderRepository,
  private val tapInBuilderFactory: OffenderScheduledTapInBuilderFactory,
) {
  fun builder() = OffenderScheduledTapOutBuilder(repository, tapInBuilderFactory)
}

class OffenderScheduledTapOutBuilder(
  private val repository: OffenderScheduledTapOutBuilderRepository,
  private val tapInBuilderFactory: OffenderScheduledTapInBuilderFactory,
) : OffenderScheduledTapOutDsl {

  private lateinit var scheduledTapOut: OffenderScheduledTapOut

  fun build(
    offenderBooking: OffenderBooking,
    eventDate: LocalDate?,
    startTime: LocalDateTime?,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String,
    prison: String,
    transportType: String,
    returnDate: LocalDate,
    returnTime: LocalDateTime,
  ): OffenderScheduledTapOut = OffenderScheduledTapOut(
    offenderBooking = offenderBooking,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = repository.movementReasonOf(eventSubType),
    eventStatus = repository.eventStatusOf(eventStatus),
    comment = comment,
    escort = repository.escortOf(escort),
    prison = repository.agencyLocationOf(prison),
    transportType = repository.tapTransportTypeOf(transportType),
    returnDate = returnDate,
    returnTime = returnTime,
  )
    .also { scheduledTapOut = it }

  override fun scheduledTapIn(
    eventDate: LocalDate,
    startTime: LocalDateTime,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String,
    toPrison: String,
  ): OffenderScheduledTapIn = tapInBuilderFactory.builder().build(
    offenderBooking = scheduledTapOut.offenderBooking,
    scheduledTapOut = scheduledTapOut,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = eventSubType,
    eventStatus = eventStatus,
    comment = comment,
    escort = escort,
    toPrison = toPrison,
  )
    .also { scheduledTapOut.scheduledTapIn = it }
}

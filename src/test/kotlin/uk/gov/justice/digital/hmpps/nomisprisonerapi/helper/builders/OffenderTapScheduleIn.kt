package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTapScheduleInDslMarker

@NomisDataDslMarker
interface OffenderTapScheduleInDsl {

  @OffenderExternalMovementDslMarker
  fun tapMovementIn(
    date: LocalDateTime = LocalDateTime.now(),
    fromAgency: String? = null,
    toPrison: String = "BXI",
    movementReason: String = "C5",
    escort: String? = null,
    escortText: String? = null,
    comment: String? = null,
    fromCity: String? = null,
    fromAddress: Address? = null,
  ): OffenderTapMovementIn
}

@Component
class OffenderTapScheduleInBuilderRepository(
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun eventStatusOf(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderTapScheduleInBuilderFactory(
  private val repository: OffenderTapScheduleInBuilderRepository,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) {
  fun builder() = OffenderTapScheduleInBuilder(repository, externalMovementBuilderFactory)
}

class OffenderTapScheduleInBuilder(
  private val repository: OffenderTapScheduleInBuilderRepository,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) : OffenderTapScheduleInDsl {

  private lateinit var tapScheduleIn: OffenderTapScheduleIn

  fun build(
    offenderBooking: OffenderBooking,
    eventDate: LocalDate,
    startTime: LocalDateTime,
    eventSubType: String,
    eventStatus: String,
    comment: String? = null,
    escort: String,
    fromAgency: String?,
    toPrison: String,
    tapScheduleOut: OffenderTapScheduleOut,
  ): OffenderTapScheduleIn = OffenderTapScheduleIn(
    offenderBooking = offenderBooking,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = repository.movementReasonOf(eventSubType),
    eventStatus = repository.eventStatusOf(eventStatus),
    comment = comment,
    escort = repository.escortOf(escort),
    fromAgency = fromAgency?.let { repository.agencyLocationOf(fromAgency) },
    toPrison = repository.agencyLocationOf(toPrison),
    tapScheduleOut = tapScheduleOut,
  )
    .also { tapScheduleIn = it }

  override fun tapMovementIn(
    date: LocalDateTime,
    fromAgency: String?,
    toPrison: String,
    movementReason: String,
    escort: String?,
    escortText: String?,
    comment: String?,
    fromCity: String?,
    fromAddress: Address?,
  ): OffenderTapMovementIn = externalMovementBuilderFactory.builder()
    .buildTapMovementIn(
      offenderBooking = tapScheduleIn.offenderBooking,
      date = date,
      fromAgency = fromAgency,
      toPrison = toPrison,
      movementReason = movementReason,
      escort = escort,
      escortText = escortText,
      comment = comment,
      fromCity = fromCity,
      fromAddress = fromAddress,
      tapScheduleIn = tapScheduleIn,
    )
    .also {
      tapScheduleIn.tapMovementIn = it
      tapScheduleIn.offenderBooking.externalMovements += it
    }
}

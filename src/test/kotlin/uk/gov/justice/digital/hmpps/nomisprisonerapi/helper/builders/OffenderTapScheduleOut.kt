package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTapScheduleOutDslMarker

@NomisDataDslMarker
interface OffenderTapScheduleOutDsl {
  @OffenderTapScheduleInDslMarker
  fun tapScheduleIn(
    eventDate: LocalDate = LocalDate.now().plusDays(1),
    startTime: LocalDateTime = eventDate.atTime(18, 0),
    eventSubType: String = "R25",
    eventStatus: String = "SCH",
    comment: String? = "Tapped IN",
    escort: String = "U",
    fromAgency: String? = null,
    toPrison: String = "LEI",
    dsl: OffenderTapScheduleInDsl.() -> Unit = {},
  ): OffenderTapScheduleIn

  @OffenderExternalMovementDslMarker
  fun tapMovementOut(
    date: LocalDateTime = LocalDateTime.now(),
    fromPrison: String = "BXI",
    toAgency: String? = null,
    movementReason: String = "C5",
    arrestAgency: String? = null,
    escort: String? = null,
    escortText: String? = null,
    comment: String? = null,
    toCity: String? = null,
    toAddress: Address? = null,
  ): OffenderTapMovementOut
}

@Component
class OffenderTapScheduleOutBuilderRepository(
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
class OffenderTapScheduleOutBuilderFactory(
  private val repository: OffenderTapScheduleOutBuilderRepository,
  private val returnBuilderFactory: OffenderTapScheduleInBuilderFactory,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) {
  fun builder() = OffenderTapScheduleOutBuilder(repository, returnBuilderFactory, externalMovementBuilderFactory)
}

class OffenderTapScheduleOutBuilder(
  private val repository: OffenderTapScheduleOutBuilderRepository,
  private val tapScheduleInBuilderFactory: OffenderTapScheduleInBuilderFactory,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) : OffenderTapScheduleOutDsl {

  private lateinit var tapScheduleOut: OffenderTapScheduleOut

  fun build(
    tapApplication: OffenderTapApplication,
    eventDate: LocalDate,
    startTime: LocalDateTime,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String,
    fromPrison: String,
    toAgency: String?,
    transportType: String,
    returnDate: LocalDate,
    returnTime: LocalDateTime,
    toAddress: Address?,
    contactPersonName: String?,
  ): OffenderTapScheduleOut = OffenderTapScheduleOut(
    tapApplication = tapApplication,
    offenderBooking = tapApplication.offenderBooking,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = repository.movementReasonOf(eventSubType),
    eventStatus = repository.eventStatusOf(eventStatus),
    comment = comment,
    escort = repository.escortOf(escort),
    fromPrison = repository.agencyLocationOf(fromPrison),
    toAgency = toAgency?.let { repository.agencyLocationOf(toAgency) },
    transportType = repository.tapTransportTypeOf(transportType),
    returnDate = returnDate,
    returnTime = returnTime,
    toAddress = toAddress,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    applicationDate = tapApplication.applicationDate,
    applicationTime = tapApplication.applicationTime,
    contactPersonName = contactPersonName,
  )
    .also { tapScheduleOut = it }

  override fun tapScheduleIn(
    eventDate: LocalDate,
    startTime: LocalDateTime,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String,
    fromAgency: String?,
    toPrison: String,
    dsl: OffenderTapScheduleInDsl.() -> Unit,
  ): OffenderTapScheduleIn = tapScheduleInBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = tapScheduleOut.offenderBooking,
      tapScheduleOut = tapScheduleOut,
      eventDate = eventDate,
      startTime = startTime,
      eventSubType = eventSubType,
      eventStatus = eventStatus,
      comment = comment,
      escort = escort,
      fromAgency = fromAgency,
      toPrison = toPrison,
    )
      .also { tapScheduleOut.tapScheduleIns += it }
      .also { builder.apply(dsl) }
  }

  override fun tapMovementOut(
    date: LocalDateTime,
    fromPrison: String,
    toAgency: String?,
    movementReason: String,
    arrestAgency: String?,
    escort: String?,
    escortText: String?,
    comment: String?,
    toCity: String?,
    toAddress: Address?,
  ): OffenderTapMovementOut = externalMovementBuilderFactory.builder()
    .buildTapMovementOut(
      offenderBooking = tapScheduleOut.offenderBooking,
      date = date,
      fromPrison = fromPrison,
      toAgency = toAgency,
      movementReason = movementReason,
      arrestAgency = arrestAgency,
      escort = escort,
      escortText = escortText,
      comment = comment,
      toCity = toCity,
      toAddress = toAddress,
      tapScheduleOut = tapScheduleOut,
    )
    .also {
      tapScheduleOut.tapMovementOut = it
      tapScheduleOut.offenderBooking.externalMovements += it
    }
}

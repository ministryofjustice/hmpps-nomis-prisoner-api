package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.persistence.DiscriminatorValue
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderScheduledTemporaryAbsenceDslMarker

@NomisDataDslMarker
interface OffenderScheduledTemporaryAbsenceDsl {
  @OffenderScheduledTemporaryAbsenceReturnDslMarker
  fun scheduledReturn(
    eventDate: LocalDate = LocalDate.now().plusDays(1),
    startTime: LocalDateTime = LocalDateTime.now().plusDays(1),
    eventSubType: String = "R25",
    eventStatus: String = "SCH",
    comment: String? = "Tapped IN",
    escort: String = "U",
    fromAgency: String? = null,
    toPrison: String = "LEI",
    dsl: OffenderScheduledTemporaryAbsenceReturnDsl.() -> Unit = {},
  ): OffenderScheduledTemporaryAbsenceReturn

  @OffenderExternalMovementDslMarker
  fun externalMovement(
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
  ): OffenderTemporaryAbsence
}

@Component
class OffenderScheduledTemporaryAbsenceBuilderRepository(
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val temporaryAbsenceTransportTypeRepository: ReferenceCodeRepository<TemporaryAbsenceTransportType>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun eventStatusOf(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun temporaryAbsenceTransportTypeOf(code: String): TemporaryAbsenceTransportType = temporaryAbsenceTransportTypeRepository.findByIdOrNull(TemporaryAbsenceTransportType.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderScheduledTemporaryAbsenceBuilderFactory(
  private val repository: OffenderScheduledTemporaryAbsenceBuilderRepository,
  private val returnBuilderFactory: OffenderScheduledTemporaryAbsenceReturnBuilderFactory,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) {
  fun builder() = OffenderScheduledTemporaryAbsenceBuilder(repository, returnBuilderFactory, externalMovementBuilderFactory)
}

class OffenderScheduledTemporaryAbsenceBuilder(
  private val repository: OffenderScheduledTemporaryAbsenceBuilderRepository,
  private val temporaryAbsenceReturnBuilderFactory: OffenderScheduledTemporaryAbsenceReturnBuilderFactory,
  private val externalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
) : OffenderScheduledTemporaryAbsenceDsl {

  private lateinit var scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence

  // I think I'll need this when writing to NOMIS during the sync... can't get access to the OWNER_CLASS column because it's a discriminator.
  private fun Address?.toAddressOwnerClass() = this?.javaClass?.getAnnotation(DiscriminatorValue::class.java)?.value

  fun build(
    temporaryAbsenceApplication: OffenderMovementApplication,
    eventDate: LocalDate?,
    startTime: LocalDateTime?,
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
  ): OffenderScheduledTemporaryAbsence = OffenderScheduledTemporaryAbsence(
    temporaryAbsenceApplication = temporaryAbsenceApplication,
    offenderBooking = temporaryAbsenceApplication.offenderBooking,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = repository.movementReasonOf(eventSubType),
    eventStatus = repository.eventStatusOf(eventStatus),
    comment = comment,
    escort = repository.escortOf(escort),
    fromPrison = repository.agencyLocationOf(fromPrison),
    toAgency = toAgency?.let { repository.agencyLocationOf(toAgency) },
    transportType = repository.temporaryAbsenceTransportTypeOf(transportType),
    returnDate = returnDate,
    returnTime = returnTime,
    toAddress = toAddress,
    toAddressOwnerClass = toAddress.toAddressOwnerClass(),
    applicationDate = temporaryAbsenceApplication.applicationDate,
    applicationTime = temporaryAbsenceApplication.applicationTime,
  )
    .also { scheduledTemporaryAbsence = it }

  override fun scheduledReturn(
    eventDate: LocalDate,
    startTime: LocalDateTime,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String,
    fromAgency: String?,
    toPrison: String,
    dsl: OffenderScheduledTemporaryAbsenceReturnDsl.() -> Unit,
  ): OffenderScheduledTemporaryAbsenceReturn = temporaryAbsenceReturnBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = scheduledTemporaryAbsence.offenderBooking,
      scheduledTemporaryAbsence = scheduledTemporaryAbsence,
      eventDate = eventDate,
      startTime = startTime,
      eventSubType = eventSubType,
      eventStatus = eventStatus,
      comment = comment,
      escort = escort,
      fromAgency = fromAgency,
      toPrison = toPrison,
    )
      .also { scheduledTemporaryAbsence.scheduledTemporaryAbsenceReturn = it }
      .also { builder.apply(dsl) }
  }

  override fun externalMovement(
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
  ): OffenderTemporaryAbsence = externalMovementBuilderFactory.builder()
    .buildTemporaryAbsence(
      offenderBooking = scheduledTemporaryAbsence.offenderBooking,
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
      scheduledTemporaryAbsence = scheduledTemporaryAbsence,
    )
    .also {
      scheduledTemporaryAbsence.temporaryAbsence = it
      scheduledTemporaryAbsence.offenderBooking.externalMovements += it
    }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementApplicationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TemporaryAbsenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTemporaryAbsenceApplicationDslMarker

@NomisDataDslMarker
interface OffenderTemporaryAbsenceApplicationDsl {
  @OffenderScheduledTemporaryAbsenceDslMarker
  fun scheduledTemporaryAbsence(
    eventDate: LocalDate = LocalDate.now(),
    startTime: LocalDateTime = eventDate.atTime(12, 0),
    eventSubType: String = "C5",
    eventStatus: String = "COMP",
    comment: String? = "Tapped OUT",
    escort: String = "A",
    fromPrison: String = "LEI",
    toAgency: String? = null,
    transportType: String = "VAN",
    returnDate: LocalDate = eventDate,
    returnTime: LocalDateTime = startTime.plusHours(1),
    toAddress: Address? = null,
    contactPersonName: String? = null,
    dsl: OffenderScheduledTemporaryAbsenceDsl.() -> Unit = {},
  ): OffenderScheduledTemporaryAbsence
}

@Component
class OffenderTemporaryAbsenceApplicationBuilderRepository(
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val applicationStatusRepository: ReferenceCodeRepository<MovementApplicationStatus>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val transportTypeRepository: ReferenceCodeRepository<TemporaryAbsenceTransportType>,
  private val applicationTypeRepository: ReferenceCodeRepository<MovementApplicationType>,
  private val temporaryAbsenceTypeRepository: ReferenceCodeRepository<TemporaryAbsenceType>,
  private val temporaryAbsenceSubTypeRepository: ReferenceCodeRepository<TemporaryAbsenceSubType>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun applicationStatusOf(code: String): MovementApplicationStatus = applicationStatusRepository.findByIdOrNull(MovementApplicationStatus.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun transportTypeOf(code: String): TemporaryAbsenceTransportType = transportTypeRepository.findByIdOrNull(TemporaryAbsenceTransportType.pk(code))!!
  fun applicationTypeOf(code: String): MovementApplicationType = applicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(code))!!
  fun temporaryAbsenceTypeOf(code: String): TemporaryAbsenceType = temporaryAbsenceTypeRepository.findByIdOrNull(TemporaryAbsenceType.pk(code))!!
  fun temporaryAbsenceSubTypeOf(code: String): TemporaryAbsenceSubType = temporaryAbsenceSubTypeRepository.findByIdOrNull(TemporaryAbsenceSubType.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderTemporaryAbsenceApplicationBuilderFactory(
  private val repository: OffenderTemporaryAbsenceApplicationBuilderRepository,
  private val scheduledTemporaryAbsenceBuilderFactory: OffenderScheduledTemporaryAbsenceBuilderFactory,
) {
  fun builder() = OffenderTemporaryAbsenceApplicationBuilder(repository, scheduledTemporaryAbsenceBuilderFactory)
}

class OffenderTemporaryAbsenceApplicationBuilder(
  private val repository: OffenderTemporaryAbsenceApplicationBuilderRepository,
  private val scheduleTemporaryAbsenceBuilderFactory: OffenderScheduledTemporaryAbsenceBuilderFactory,
) : OffenderTemporaryAbsenceApplicationDsl {

  private lateinit var temporaryAbsenceApplication: OffenderMovementApplication

  fun build(
    offenderBooking: OffenderBooking,
    eventSubType: String,
    applicationDate: LocalDateTime,
    applicationTime: LocalDateTime,
    fromDate: LocalDate,
    releaseTime: LocalDateTime,
    toDate: LocalDate,
    returnTime: LocalDateTime,
    applicationStatus: String,
    escort: String?,
    transportType: String?,
    comment: String?,
    toAddress: Address?,
    prison: String,
    toAgency: String?,
    contactPersonName: String?,
    applicationType: String,
    temporaryAbsenceType: String?,
    temporaryAbsenceSubType: String?,
  ): OffenderMovementApplication = OffenderMovementApplication(
    offenderBooking = offenderBooking,
    eventSubType = repository.movementReasonOf(eventSubType),
    applicationDate = applicationDate,
    applicationTime = applicationTime,
    fromDate = fromDate,
    releaseTime = releaseTime,
    toDate = toDate,
    returnTime = returnTime,
    applicationStatus = repository.applicationStatusOf(applicationStatus),
    escort = escort?.let { repository.escortOf(it) },
    transportType = transportType?.let { repository.transportTypeOf(it) },
    comment = comment,
    toAddress = toAddress,
    toAddressOwnerClass = toAddress?.addressOwnerClass,
    prison = repository.agencyLocationOf(prison),
    toAgency = toAgency?.let { repository.agencyLocationOf(it) },
    contactPersonName = contactPersonName,
    applicationType = repository.applicationTypeOf(applicationType),
    temporaryAbsenceType = temporaryAbsenceType?.let { repository.temporaryAbsenceTypeOf(it) },
    temporaryAbsenceSubType = temporaryAbsenceSubType?.let { repository.temporaryAbsenceSubTypeOf(it) },
  )
    .also { temporaryAbsenceApplication = it }

  override fun scheduledTemporaryAbsence(
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
    dsl: OffenderScheduledTemporaryAbsenceDsl.() -> Unit,
  ): OffenderScheduledTemporaryAbsence = scheduleTemporaryAbsenceBuilderFactory.builder().let { builder ->
    builder.build(
      temporaryAbsenceApplication = temporaryAbsenceApplication,
      eventDate = eventDate,
      startTime = startTime,
      eventSubType = eventSubType,
      eventStatus = eventStatus,
      comment = comment,
      escort = escort,
      fromPrison = fromPrison,
      toAgency = toAgency,
      transportType = transportType,
      returnDate = returnDate,
      returnTime = returnTime,
      toAddress = toAddress,
      contactPersonName = contactPersonName,
    )
      .also { temporaryAbsenceApplication.scheduledTemporaryAbsences += it }
      .also { builder.apply(dsl) }
  }
}

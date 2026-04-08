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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapTransportType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TapType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTapApplicationDslMarker

@NomisDataDslMarker
interface OffenderTapApplicationDsl {
  @OffenderTapScheduleOutDslMarker
  fun tapScheduleOut(
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
    dsl: OffenderTapScheduleOutDsl.() -> Unit = {},
  ): OffenderTapScheduleOut
}

@Component
class OffenderTapApplicationBuilderRepository(
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val applicationStatusRepository: ReferenceCodeRepository<MovementApplicationStatus>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val transportTypeRepository: ReferenceCodeRepository<TapTransportType>,
  private val applicationTypeRepository: ReferenceCodeRepository<MovementApplicationType>,
  private val tapTypeRepository: ReferenceCodeRepository<TapType>,
  private val tapSubTypeRepository: ReferenceCodeRepository<TapSubType>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun applicationStatusOf(code: String): MovementApplicationStatus = applicationStatusRepository.findByIdOrNull(MovementApplicationStatus.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun transportTypeOf(code: String): TapTransportType = transportTypeRepository.findByIdOrNull(TapTransportType.pk(code))!!
  fun applicationTypeOf(code: String): MovementApplicationType = applicationTypeRepository.findByIdOrNull(MovementApplicationType.pk(code))!!
  fun tapTypeOf(code: String): TapType = tapTypeRepository.findByIdOrNull(TapType.pk(code))!!
  fun tapSubTypeOf(code: String): TapSubType = tapSubTypeRepository.findByIdOrNull(TapSubType.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderTapApplicationBuilderFactory(
  private val repository: OffenderTapApplicationBuilderRepository,
  private val tapScheduleOutBuilderFactory: OffenderTapScheduleOutBuilderFactory,
) {
  fun builder() = OffenderTapApplicationBuilder(repository, tapScheduleOutBuilderFactory)
}

class OffenderTapApplicationBuilder(
  private val repository: OffenderTapApplicationBuilderRepository,
  private val tapScheduleOutBuilderFactory: OffenderTapScheduleOutBuilderFactory,
) : OffenderTapApplicationDsl {

  private lateinit var tapApplication: OffenderTapApplication

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
    tapType: String?,
    tapSubType: String?,
  ): OffenderTapApplication = OffenderTapApplication(
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
    tapType = tapType?.let { repository.tapTypeOf(it) },
    tapSubType = tapSubType?.let { repository.tapSubTypeOf(it) },
  )
    .also { tapApplication = it }

  override fun tapScheduleOut(
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
    dsl: OffenderTapScheduleOutDsl.() -> Unit,
  ): OffenderTapScheduleOut = tapScheduleOutBuilderFactory.builder().let { builder ->
    builder.build(
      tapApplication = tapApplication,
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
      .also { tapApplication.tapScheduleOuts += it }
      .also { builder.apply(dsl) }
  }
}

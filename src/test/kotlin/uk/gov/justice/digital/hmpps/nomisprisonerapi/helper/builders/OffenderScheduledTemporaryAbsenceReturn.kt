package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderScheduledTemporaryAbsenceReturnDslMarker

@NomisDataDslMarker
interface OffenderScheduledTemporaryAbsenceReturnDsl

@Component
class OffenderScheduledTemporaryAbsenceReturnBuilderRepository(
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
class OffenderScheduledTemporaryAbsenceReturnBuilderFactory(
  private val repository: OffenderScheduledTemporaryAbsenceReturnBuilderRepository,
) {
  fun builder() = OffenderScheduledTemporaryAbsenceReturnBuilder(repository)
}

class OffenderScheduledTemporaryAbsenceReturnBuilder(private val repository: OffenderScheduledTemporaryAbsenceReturnBuilderRepository) : OffenderScheduledTemporaryAbsenceReturnDsl {
  fun build(
    offenderBooking: OffenderBooking,
    eventDate: LocalDate? = null,
    startTime: LocalDateTime? = null,
    eventSubType: String,
    eventStatus: String,
    comment: String? = null,
    escort: String,
    toPrison: String,
    scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence,
  ): OffenderScheduledTemporaryAbsenceReturn = OffenderScheduledTemporaryAbsenceReturn(
    offenderBooking = offenderBooking,
    eventDate = eventDate,
    startTime = startTime,
    eventSubType = repository.movementReasonOf(eventSubType),
    eventStatus = repository.eventStatusOf(eventStatus),
    comment = comment,
    escort = repository.escortOf(escort),
    toPrison = repository.agencyLocationOf(toPrison),
    scheduledTemporaryAbsence = scheduledTemporaryAbsence,
  )
}

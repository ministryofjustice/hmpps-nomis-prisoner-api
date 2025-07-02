package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTemporaryAbsenceApplicationMovementsDslMarker

@OffenderTemporaryAbsenceApplicationMovementsDslMarker
interface OffenderTemporaryAbsenceApplicationMovementsDsl

@Component
class OffenderTemporaryAbsenceApplicationMovementsBuilderFactory(
  private val repository: OffenderTemporaryAbsenceApplicationBuilderRepository,
) {
  fun builder() = OffenderTemporaryAbsenceApplicationMovementsBuilder(repository)
}

class OffenderTemporaryAbsenceApplicationMovementsBuilder(
  private val repository: OffenderTemporaryAbsenceApplicationBuilderRepository,
) : OffenderTemporaryAbsenceApplicationMovementsDsl {

  fun build(
    offenderMovementApplication: OffenderMovementApplication,
    eventSubType: String,
    fromDate: LocalDate,
    releaseTime: LocalDateTime,
    toDate: LocalDate,
    returnTime: LocalDateTime,
    comment: String?,
    toAgency: String?,
    contactPersonName: String?,
    temporaryAbsenceType: String?,
    temporaryAbsenceSubType: String?,
  ): OffenderMovementApplicationMulti = OffenderMovementApplicationMulti(
    offenderMovementApplication = offenderMovementApplication,
    eventSubType = repository.movementReasonOf(eventSubType),
    fromDate = fromDate,
    releaseTime = releaseTime,
    toDate = toDate,
    returnTime = returnTime,
    comment = comment,
    toAgency = toAgency?.let { repository.agencyLocationOf(it) },
    contactPersonName = contactPersonName,
    temporaryAbsenceType = temporaryAbsenceType?.let { repository.temporaryAbsenceTypeOf(it) },
    temporaryAbsenceSubType = temporaryAbsenceSubType?.let { repository.temporaryAbsenceSubTypeOf(it) },
  )
}

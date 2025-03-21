package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.IN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.OUT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DslMarker
annotation class OffenderExternalMovementDslMarker

@NomisDataDslMarker
interface OffenderExternalMovementDsl

@Component
class OffenderExternalMovementBuilderFactory(
  private val repository: OffenderExternalMovementBuilderRepository,
) {
  fun builder(): OffenderExternalMovementBuilder = OffenderExternalMovementBuilder(repository)
}

@Component
class OffenderExternalMovementBuilderRepository(
  val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  val movementTypeRepository: ReferenceCodeRepository<MovementType>,
  val agencyLocationRepository: AgencyLocationRepository,
  val offenderExternalMovementRepository: OffenderExternalMovementRepository,
) {
  fun lookupMovementReason(code: String): MovementReason = movementReasonRepository.findByIdOrNull(Pk(MovementReason.MOVE_RSN, code))!!

  fun lookupMovementType(code: String): MovementType = movementTypeRepository.findByIdOrNull(Pk(MovementType.MOVE_TYPE, code))!!

  fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId)!!

  fun save(movement: OffenderExternalMovement): OffenderExternalMovement = offenderExternalMovementRepository.save(movement)
}

class OffenderExternalMovementBuilder(
  private val repository: OffenderExternalMovementBuilderRepository,
) : OffenderExternalMovementDsl {
  fun buildTransfer(
    fromPrisonId: String,
    toPrisonId: String,
    date: LocalDateTime,
    offenderBooking: OffenderBooking,
  ): Pair<OffenderExternalMovement, OffenderExternalMovement> = repository.save(
    OffenderExternalMovement(
      id = OffenderExternalMovementId(
        offenderBooking,
        offenderBooking.externalMovements.size + 1L,
      ),
      movementDate = date.toLocalDate(),
      movementTime = date,
      movementDirection = OUT,
      movementType = repository.lookupMovementType("TRN"),
      movementReason = repository.lookupMovementReason("28"),
      fromAgency = repository.lookupAgency(fromPrisonId),
      toAgency = repository.lookupAgency(toPrisonId),
      active = false,
    ),
  ) to repository.save(
    OffenderExternalMovement(
      id = OffenderExternalMovementId(
        offenderBooking,
        offenderBooking.externalMovements.size + 1L,
      ),
      movementDate = date.toLocalDate(),
      movementTime = date.plusSeconds(1),
      movementDirection = IN,
      movementType = repository.lookupMovementType("ADM"),
      movementReason = repository.lookupMovementReason("INT"),
      fromAgency = repository.lookupAgency(fromPrisonId),
      toAgency = repository.lookupAgency(toPrisonId),
      active = true,
    ),
  )
  fun buildRelease(
    date: LocalDateTime,
    offenderBooking: OffenderBooking,
  ): OffenderExternalMovement = repository.save(
    OffenderExternalMovement(
      id = OffenderExternalMovementId(
        offenderBooking,
        offenderBooking.externalMovements.size + 1L,
      ),
      movementDate = date.toLocalDate(),
      movementTime = date,
      movementDirection = OUT,
      movementType = repository.lookupMovementType("REL"),
      movementReason = repository.lookupMovementReason("CR"),
      fromAgency = offenderBooking.location,
      toAgency = null,
      active = true,
    ),
  ).also {
    offenderBooking.inOutStatus = "OUT"
    offenderBooking.location = repository.lookupAgency("OUT")
    offenderBooking.active = false
    if (offenderBooking.bookingEndDate == null) {
      offenderBooking.bookingEndDate = date.truncatedTo(ChronoUnit.DAYS)
    }
  }

  fun buildReceive(
    date: LocalDateTime,
    offenderBooking: OffenderBooking,
  ): OffenderExternalMovement = repository.save(
    OffenderExternalMovement(
      id = OffenderExternalMovementId(
        offenderBooking,
        offenderBooking.externalMovements.size + 1L,
      ),
      movementDate = date.toLocalDate(),
      movementTime = date,
      movementDirection = IN,
      movementType = repository.lookupMovementType("ADM"),
      movementReason = repository.lookupMovementReason("N"),
      toAgency = offenderBooking.location,
      fromAgency = null,
      active = false,
    ),
  ).also {
    offenderBooking.inOutStatus = "IN"
    offenderBooking.location = offenderBooking.location
    offenderBooking.active = true
    offenderBooking.bookingEndDate = null
  }

  fun build(
    fromPrisonId: String,
    toPrisonId: String,
    date: LocalDateTime,
    offenderBooking: OffenderBooking,
    movementType: String,
    movementReason: String,
  ): OffenderExternalMovement = repository.save(
    OffenderExternalMovement(
      id = OffenderExternalMovementId(
        offenderBooking,
        offenderBooking.externalMovements.size + 1L,
      ),
      movementDate = date.toLocalDate(),
      movementTime = date,
      movementDirection = IN,
      movementType = repository.lookupMovementType(movementType),
      movementReason = repository.lookupMovementReason(movementReason),
      fromAgency = repository.lookupAgency(fromPrisonId),
      toAgency = repository.lookupAgency(toPrisonId),
      active = true,
    ),
  )
}

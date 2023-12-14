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

@DslMarker
annotation class OffenderExternalMovementDslMarker

@NomisDataDslMarker
interface OffenderExternalMovementDsl

@Component
class OffenderExternalMovementBuilderFactory(
  private val repository: OffenderExternalMovementBuilderRepository,
) {
  fun builder(): OffenderExternalMovementBuilder {
    return OffenderExternalMovementBuilder(repository)
  }
}

@Component
class OffenderExternalMovementBuilderRepository(
  val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  val movementTypeRepository: ReferenceCodeRepository<MovementType>,
  val agencyLocationRepository: AgencyLocationRepository,
  val offenderExternalMovementRepository: OffenderExternalMovementRepository,
) {
  fun lookupMovementReason(code: String): MovementReason =
    movementReasonRepository.findByIdOrNull(Pk(MovementReason.MOVE_RSN, code))!!

  fun lookupMovementType(code: String): MovementType =
    movementTypeRepository.findByIdOrNull(Pk(MovementType.MOVE_TYPE, code))!!

  fun lookupPrison(prisonId: String): AgencyLocation =
    agencyLocationRepository.findByIdOrNull(prisonId)!!

  fun save(movement: OffenderExternalMovement): OffenderExternalMovement =
    offenderExternalMovementRepository.save(movement)
}

class OffenderExternalMovementBuilder(
  private val repository: OffenderExternalMovementBuilderRepository,
) : OffenderExternalMovementDsl {
  fun buildTransfer(
    fromPrisonId: String,
    toPrisonId: String,
    date: LocalDateTime,
    offenderBooking: OffenderBooking,
  ): Pair<OffenderExternalMovement, OffenderExternalMovement> =
    repository.save(
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
        fromAgency = repository.lookupPrison(fromPrisonId),
        toAgency = repository.lookupPrison(toPrisonId),
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
        fromAgency = repository.lookupPrison(fromPrisonId),
        toAgency = repository.lookupPrison(toPrisonId),
        active = true,
      ),
    )
}

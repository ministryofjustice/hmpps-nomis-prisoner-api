package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import java.time.LocalDateTime

@Service
class ExternalMovementService(
  private val externalMovementRepository: OffenderExternalMovementRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {

  fun findPrisonAt(time: LocalDateTime, offenderNo: String): AgencyLocation? {
    val movements = externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offenderNo)
      .filter { it.movementReason.id.type == "ADM" || it.movementReason.id.type == "REL" }
    return movements
      .filter { it.getMovementDateAndTime() < time }
      .maxByOrNull { it.getMovementDateAndTime() }
      ?.takeIf { it.movementReason.id.type == "ADM" }
      ?.toAgency
      ?: agencyLocationRepository.findByIdOrNull("OUT")
  }
}

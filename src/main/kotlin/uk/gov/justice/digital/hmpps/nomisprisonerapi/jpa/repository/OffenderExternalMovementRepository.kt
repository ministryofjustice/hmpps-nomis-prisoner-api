package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId

@Repository
interface OffenderExternalMovementRepository :
  CrudRepository<OffenderExternalMovement, OffenderExternalMovementId> {
  @Query(
    """
      select 
        distinct movement.toAgency.id
      from OffenderExternalMovement movement 
      where
        movement.id.offenderBooking = :booking and
        movement.movementType.code = 'ADM' 
      order by movement.toAgency.id asc 
    """,
  )
  fun findPrisonsAdmittedIntoByBooking(booking: OffenderBooking): List<String>
}

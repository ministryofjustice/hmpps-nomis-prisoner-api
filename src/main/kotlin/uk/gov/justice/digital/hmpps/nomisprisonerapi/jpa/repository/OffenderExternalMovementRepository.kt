package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId

@Repository
interface OffenderExternalMovementRepository : CrudRepository<OffenderExternalMovement, OffenderExternalMovementId> {
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

  @Query(
    """
      select 
        case 
          when oem.EVENT_ID is not null OR oem.PARENT_EVENT_ID is not null then 'Y' 
          else 'N' 
        end as SCHEDULED, 
        DIRECTION_CODE, 
        count(*) 
      from OFFENDERS o 
        join OFFENDER_BOOKINGS ob on o.offender_id=ob.root_offender_id
        join OFFENDER_EXTERNAL_MOVEMENTS oem on ob.OFFENDER_BOOK_ID=oem.OFFENDER_BOOK_ID
      where oem.MOVEMENT_TYPE='TAP' and o.OFFENDER_ID_DISPLAY=:offender
      group by 
        case 
          when oem.EVENT_ID is not null OR oem.PARENT_EVENT_ID is not null then 'Y' 
          else 'N' 
        end, 
        DIRECTION_CODE
    """,
    nativeQuery = true,
  )
  fun countOffenderTemporaryAbsenceMovements(offender: String): List<TemporaryAbsenceMovementCounts>
}

class TemporaryAbsenceMovementCounts(scheduledFlag: Char, directionCode: String, val count: Long) {
  val scheduled: Boolean = scheduledFlag == 'Y'
  val direction: MovementDirection = MovementDirection.valueOf(directionCode)
}

enum class MovementDirection { OUT, IN }

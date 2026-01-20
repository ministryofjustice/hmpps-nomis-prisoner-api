package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId

@Repository
interface OffenderExternalMovementRepository : CrudRepository<OffenderExternalMovement, OffenderExternalMovementId> {

  @Query(
    """
      select count(*)
      from OFFENDERS o
        join OFFENDER_BOOKINGS ob on o.offender_id=ob.root_offender_id
        join OFFENDER_EXTERNAL_MOVEMENTS oem on ob.OFFENDER_BOOK_ID=oem.OFFENDER_BOOK_ID
      where oem.MOVEMENT_TYPE='TAP' and o.OFFENDER_ID_DISPLAY=:offender and oem.DIRECTION_CODE = 'OUT' and oem.event_id is null
    """,
    nativeQuery = true,
  )
  fun countOffenderUnscheduledOut(offender: String): Long

  @Query(
    """
      select count(*)
      from OFFENDERS o
        join OFFENDER_BOOKINGS ob on o.offender_id=ob.root_offender_id
        join OFFENDER_EXTERNAL_MOVEMENTS oem on ob.OFFENDER_BOOK_ID=oem.OFFENDER_BOOK_ID
      where oem.MOVEMENT_TYPE='TAP' and o.OFFENDER_ID_DISPLAY=:offender and oem.DIRECTION_CODE = 'IN' and oem.event_id is null
    """,
    nativeQuery = true,
  )
  fun countOffenderUnscheduledIn(offender: String): Long

  @Query(
    """
      select count(*)
      from OFFENDERS o
        join OFFENDER_BOOKINGS ob on o.offender_id=ob.root_offender_id
        join OFFENDER_EXTERNAL_MOVEMENTS oem on ob.OFFENDER_BOOK_ID=oem.OFFENDER_BOOK_ID
        -- the scheduled OUT movement must exist to be included - some have been deleted
        join OFFENDER_IND_SCHEDULES ois on oem.EVENT_ID=ois.EVENT_ID
      where oem.MOVEMENT_TYPE='TAP' and o.OFFENDER_ID_DISPLAY=:offender and oem.DIRECTION_CODE = 'OUT'
        and ois.OFFENDER_MOVEMENT_APP_ID is not null
    """,
    nativeQuery = true,
  )
  fun countOffenderScheduledOut(offender: String): Long

  @Query(
    """
      select count(*)
      from OFFENDERS o
        join OFFENDER_BOOKINGS ob on o.offender_id=ob.root_offender_id
        join OFFENDER_EXTERNAL_MOVEMENTS oem on ob.OFFENDER_BOOK_ID=oem.OFFENDER_BOOK_ID
        -- the scheduled OUT and IN movements must exist to be included - some have been deleted
        join OFFENDER_IND_SCHEDULES ois_in on oem.EVENT_ID=ois_in.EVENT_ID
        left join OFFENDER_IND_SCHEDULES ois_out on oem.PARENT_EVENT_ID=ois_out.EVENT_ID
      where oem.MOVEMENT_TYPE='TAP' and o.OFFENDER_ID_DISPLAY=:offender and oem.DIRECTION_CODE = 'IN'
      -- we only care about the join to scheduled OUT movement if it is linked from the actual movement IN
      and ( oem.PARENT_EVENT_ID IS NULL or (ois_out.EVENT_ID IS NOT NULL and ois_out.OFFENDER_MOVEMENT_APP_ID is not null))
    """,
    nativeQuery = true,
  )
  fun countOffenderScheduledIn(offender: String): Long
}

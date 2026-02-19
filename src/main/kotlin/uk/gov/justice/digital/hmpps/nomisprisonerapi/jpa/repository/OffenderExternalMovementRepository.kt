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
        left join OFFENDER_IND_SCHEDULES ois on oem.EVENT_ID=ois.EVENT_ID
        left join OFFENDER_MOVEMENT_APPS oma on ois.OFFENDER_MOVEMENT_APP_ID=oma.OFFENDER_MOVEMENT_APP_ID
      where oem.MOVEMENT_TYPE='TAP' 
        and o.OFFENDER_ID_DISPLAY=:offender 
        and oem.DIRECTION_CODE = 'OUT' 
        and (
          oem.event_id is null 
          or ois.EVENT_ID is null 
          or ois.EVENT_TYPE != 'TAP'
          or oma.OFFENDER_MOVEMENT_APP_ID is null
        )
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
             left join OFFENDER_IND_SCHEDULES ois_in on oem.EVENT_ID=ois_in.EVENT_ID
             left join OFFENDER_IND_SCHEDULES ois_out on oem.PARENT_EVENT_ID=ois_out.EVENT_ID
             left join offender_movement_apps oma on ois_out.OFFENDER_MOVEMENT_APP_ID=oma.OFFENDER_MOVEMENT_APP_ID
      where oem.MOVEMENT_TYPE='TAP'
        and o.OFFENDER_ID_DISPLAY=:offender
        and oem.DIRECTION_CODE = 'IN'
        and (
          ois_in.EVENT_ID is null 
          or ois_in.EVENT_TYPE != 'TAP'
          or ois_out.EVENT_ID is null 
          or ois_out.EVENT_TYPE != 'TAP'
          or ois_in.PARENT_EVENT_ID != ois_out.EVENT_ID 
          or ois_in.PARENT_EVENT_ID != ois_out.EVENT_ID 
          or oma.OFFENDER_MOVEMENT_APP_ID is null
        )
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
        join OFFENDER_MOVEMENT_APPS oma on ois.OFFENDER_MOVEMENT_APP_ID=oma.OFFENDER_MOVEMENT_APP_ID
      where oem.MOVEMENT_TYPE='TAP' 
        and o.OFFENDER_ID_DISPLAY=:offender 
        and oem.DIRECTION_CODE = 'OUT'
        and ois.EVENT_TYPE = 'TAP'
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
             join OFFENDER_IND_SCHEDULES ois_in on oem.EVENT_ID=ois_in.EVENT_ID
             join OFFENDER_IND_SCHEDULES ois_out on oem.PARENT_EVENT_ID=ois_out.EVENT_ID
             join OFFENDER_MOVEMENT_APPS oma on ois_out.OFFENDER_MOVEMENT_APP_ID=oma.OFFENDER_MOVEMENT_APP_ID
      where oem.MOVEMENT_TYPE='TAP'
        and o.OFFENDER_ID_DISPLAY=:offender
        and oem.DIRECTION_CODE = 'IN'
        and ois_out.EVENT_ID = ois_in.PARENT_EVENT_ID
        and ois_out.EVENT_TYPE = 'TAP'
        and ois_in.EVENT_TYPE = 'TAP'
    """,
    nativeQuery = true,
  )
  fun countOffenderScheduledIn(offender: String): Long
}

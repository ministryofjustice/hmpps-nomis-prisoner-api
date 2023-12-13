package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import java.time.LocalDate

@Repository
interface OffenderIndividualScheduleRepository :
  CrudRepository<OffenderIndividualSchedule, Long>, JpaSpecificationExecutor<OffenderIndividualSchedule> {

  @Query(
    "from OffenderIndividualSchedule ois where ois.offenderBooking.bookingId = :bookingId and ois.internalLocation.locationId = :locationId " +
      "and ois.eventDate = :date and hour(ois.startTime) = :hour and minute(ois.startTime) = :minute",
  )
  fun findOneByBookingLocationDateAndStartTime(
    bookingId: Long,
    locationId: Long,
    date: LocalDate,
    hour: Int,
    minute: Int,
  ): OffenderIndividualSchedule?

  @Query(
    """SELECT event_id FROM  (
        SELECT /*+ index(offender_ind_schedules OFFENDER_IND_SCHEDULES_X03) */ 
            event_id,
            ROW_NUMBER() OVER (ORDER BY event_id) rn
        FROM
            offender_ind_schedules
        WHERE
                event_type = 'APP'
            and agy_loc_id in (:prisons)
            and event_date between :fromDate and :toDate
    ) inner_query
    WHERE
        rn between :first and :last
    ORDER BY inner_query.rn
    """,
    nativeQuery = true,
  )
  fun findAllByPage(
    prisons: List<String>,
    fromDate: LocalDate,
    toDate: LocalDate,
    first: Long,
    last: Long,
  ): List<Long>

  @Query(
    """SELECT /*+ index(offender_ind_schedules OFFENDER_IND_SCHEDULES_X03) */ 
            count(1) 
       FROM
           offender_ind_schedules
       WHERE
               event_type = 'APP'
           and agy_loc_id in (:prisons)
           and event_date between :fromDate and :toDate
    """,
    nativeQuery = true,
  )
  fun findAllCount(
    prisons: List<String>,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Long
}

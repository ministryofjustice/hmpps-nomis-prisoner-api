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

  // should use index OFFENDER_IND_SCHEDULES_X02 on OFFENDER_BOOK_ID, EVENT_DATE
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
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalTime

@Repository
interface AgencyVisitTimeRepository : JpaRepository<AgencyVisitTime, AgencyVisitTimeId> {

  fun findByAgencyVisitTimesIdLocationId(locationId: String): List<AgencyVisitTime>

  fun findByStartTimeAndAgencyVisitTimesIdWeekDayAndAgencyVisitTimesIdLocation(
    startTime: LocalTime,
    weekDay: WeekDay,
    location: AgencyLocation,
  ): AgencyVisitTime?

  // roundabout way of getting the max timeslot sequence for a prison and day
  fun findFirstByAgencyVisitTimesIdLocationAndAgencyVisitTimesIdWeekDayOrderByAgencyVisitTimesIdTimeSlotSequenceDesc(
    agencyId: AgencyLocation,
    weekDay: WeekDay,
  ): AgencyVisitTime?

  @Query(
    """
      select 
        t.agencyVisitTimesId.location.id as prisonId,
        t.agencyVisitTimesId.weekDay as weekdayCode,
        t.agencyVisitTimesId.timeSlotSequence as timeSlotSequence
      from AgencyVisitTime t 
    """,
  )
  fun findAllIds(
    pageable: Pageable,
  ): Page<VisitTimeSlotIdProjection>
}

interface VisitTimeSlotIdProjection {
  val prisonId: String
  val weekdayCode: WeekDay
  val timeSlotSequence: Int
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import java.time.LocalTime

@Repository
interface AgencyVisitTimeRepository :
  JpaRepository<AgencyVisitTime, AgencyVisitTimeId> {

  fun findByAgencyVisitTimesId_Location_Id(locationId: String): List<AgencyVisitTime>

  fun findByStartTimeAndAgencyVisitTimesId_WeekDayAndAgencyVisitTimesId_Location(
    startTime: LocalTime,
    weekDay: String,
    location: AgencyLocation,
  ): AgencyVisitTime?

  // roundabout way of getting the max timeslot sequence for a prison and day
  fun findFirstByAgencyVisitTimesId_LocationAndAgencyVisitTimesId_WeekDayOrderByAgencyVisitTimesId_TimeSlotSequenceDesc(
    agencyId: AgencyLocation,
    weekDay: String,
  ): AgencyVisitTime?
}

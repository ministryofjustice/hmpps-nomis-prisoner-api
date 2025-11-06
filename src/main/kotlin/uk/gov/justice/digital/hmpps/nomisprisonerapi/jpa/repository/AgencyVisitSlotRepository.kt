package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalTime

@Repository
interface AgencyVisitSlotRepository : CrudRepository<AgencyVisitSlot, Long> {

  fun findByLocationId(locationId: String): List<AgencyVisitSlot>

  fun findByAgencyInternalLocationDescriptionAndAgencyVisitTimeStartTimeAndWeekDay(
    roomDescription: String,
    startTime: LocalTime,
    weekDay: WeekDay,
  ): AgencyVisitSlot?
}

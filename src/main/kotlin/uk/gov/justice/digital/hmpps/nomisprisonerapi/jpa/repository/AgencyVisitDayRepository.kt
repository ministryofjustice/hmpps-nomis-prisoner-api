package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDayId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay

@Repository
interface AgencyVisitDayRepository : JpaRepository<AgencyVisitDay, AgencyVisitDayId> {

  fun findByAgencyVisitDayIdWeekDayAndAgencyVisitDayIdLocationId(
    weekDay: WeekDay,
    locationId: String,
  ): AgencyVisitDay?
}

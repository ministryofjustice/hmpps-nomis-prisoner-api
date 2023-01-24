package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import java.time.LocalDate

class CourseActivityBuilder(
  var code: String = "CA",
  var programId: Long = 20,
  var prisonId: String = "LEI",
  var description: String = "test course activity",
  var capacity: Int = 23,
  var active: Boolean = true,
  var startDate: String = "2022-10-31",
  var endDate: String = "2022-11-30",
  var minimumIncentiveLevelCode: String = "STD",
  var internalLocationId: Long = -8,
  var payRates: List<CourseActivityPayRateBuilder> = listOf(CourseActivityPayRateBuilder()),
) {
  fun build(
    prison: AgencyLocation,
    programService: ProgramService,
    minimumIepLevel: IEPLevel,
    internalLocation: AgencyInternalLocation
  ): CourseActivity {
    assert(prison.id == this.prisonId)
    assert(programService.programId == programId)
    assert(minimumIepLevel.code == minimumIncentiveLevelCode)
    assert(internalLocation.locationId == internalLocationId)
    return CourseActivity(
      code = code,
      program = programService,
      caseloadId = prison.id,
      prison = prison,
      description = description,
      capacity = capacity,
      active = active,
      scheduleStartDate = LocalDate.parse(startDate),
      scheduleEndDate = LocalDate.parse(endDate),
      iepLevel = minimumIepLevel,
      internalLocation = internalLocation,
    )
  }
}

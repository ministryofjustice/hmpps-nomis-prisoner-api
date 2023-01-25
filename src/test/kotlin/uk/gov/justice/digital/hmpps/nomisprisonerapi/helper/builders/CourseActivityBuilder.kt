package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import java.time.LocalDate

@Component
class CourseActivityBuilderFactory(private val repository: Repository) {
  fun builder(
    code: String = "CA",
    programId: Long = 20,
    prisonId: String = "LEI",
    description: String = "test course activity",
    capacity: Int = 23,
    active: Boolean = true,
    startDate: String = "2022-10-31",
    endDate: String = "2022-11-30",
    minimumIncentiveLevelCode: String = "STD",
    internalLocationId: Long = -8,
    payRates: List<CourseActivityPayRateBuilder> = listOf(CourseActivityPayRateBuilder()),
  ): CourseActivityBuilder {
    return CourseActivityBuilder(
      repository,
      code,
      programId,
      prisonId,
      description,
      capacity,
      active,
      startDate,
      endDate,
      minimumIncentiveLevelCode,
      internalLocationId,
      payRates
    )
  }
}

class CourseActivityBuilder(
  val repository: Repository,
  var code: String,
  var programId: Long,
  var prisonId: String,
  var description: String,
  var capacity: Int,
  var active: Boolean,
  var startDate: String,
  var endDate: String,
  var minimumIncentiveLevelCode: String,
  var internalLocationId: Long,
  var payRates: List<CourseActivityPayRateBuilder> = listOf(CourseActivityPayRateBuilder()),
) {
  fun build(): CourseActivity =
    CourseActivity(
      code = code,
      program = repository.lookupProgramService(programId),
      caseloadId = prisonId,
      prison = repository.lookupAgency(prisonId),
      description = description,
      capacity = capacity,
      active = active,
      scheduleStartDate = LocalDate.parse(startDate),
      scheduleEndDate = LocalDate.parse(endDate),
      iepLevel = repository.lookupIepLevel(minimumIncentiveLevelCode),
      internalLocation = repository.lookupAgencyInternalLocation(internalLocationId),
    ).apply {
      payRates.addAll(this@CourseActivityBuilder.payRates.map { it.build(this) })
    }
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class MigrationService(
  private val agencyLocationRepository: AgencyLocationRepository,
  private val courseActivityRepository: CourseActivityRepository,
) {
  fun findMigrationActivities(pageRequest: Pageable, prisonId: String): Page<FindMigrationActivitiesResponse> =
    findPrison(prisonId)
      .let { courseActivityRepository.findActivitiesToMigrate(prisonId, pageRequest) }
      .map { FindMigrationActivitiesResponse(it) }

  fun getActivityMigration(courseActivityId: Long): GetActivityMigrationResponse? =
    findCourseActivity(courseActivityId)
      .let { null }

  private fun findPrison(prisonId: String) =
    agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw NotFoundException("Prison with id=$prisonId does not exist")

  private fun findCourseActivity(courseActivityId: Long) =
    courseActivityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course Activity with id=$courseActivityId does not exist")
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find migration activities request")
data class FindMigrationActivitiesResponse(
  @Schema(description = "The activity id to be migrated", example = "1")
  val courseActivityId: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity Migration details")
data class GetActivityMigrationResponse(
  @Schema(description = "The activity id to be migrated", example = "1")
  val courseActivityId: Long,

  @Schema(description = "Program service code", example = "INDUCTION")
  val programCode: Long,

  @Schema(description = "Prison code", example = "RSI")
  val prisonId: String,

  @Schema(description = "Date course started", example = "2020-04-11")
  val startDate: LocalDate,

  @Schema(description = "Date course ended", example = "2023-11-15")
  val endDate: LocalDate?,

  @Schema(description = "Course internal location", example = "1234")
  val internalLocationId: Long?,

  @Schema(description = "Course internal location code", example = "KITCH")
  val internalLocationCode: String?,

  @Schema(description = "Course internal location description", example = "RSI-WORK_IND-KITCH")
  val internalLocationDescription: String?,

  @Schema(description = "Course capacity", example = "10")
  val capacity: Int,

  @Schema(description = "Course description", example = "Kitchen work")
  val description: String,

  @Schema(description = "The minimum incentive level allowed on the course", example = "BAS")
  val minimumIncentiveLevel: String,

  @Schema(description = "Whether the course runs on bank holidays", example = "false")
  val excludeBankHolidays: Boolean,

  @Schema(description = "Rules for creating schedules - days and times")
  val scheduleRules: List<MigrationScheduleRules>,

  @Schema(description = "Pay rates available")
  val payRate: List<MigrationPayRates>,

  @Schema(description = "Prisoners allocated to the course")
  val allocations: List<MigrationAllocations>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity Schedule Rules")
data class MigrationScheduleRules(

  @Schema(description = "Course start time", example = "09:00")
  val startTime: LocalTime,

  @Schema(description = "Course end time", example = "11:00")
  val endTime: LocalTime,

  @Schema(description = "Runs on Mondays", example = "true")
  val monday: Boolean,

  @Schema(description = "Runs on Tuesdays", example = "true")
  val tuesday: Boolean,

  @Schema(description = "Runs on Wednesdays", example = "true")
  val wednesday: Boolean,

  @Schema(description = "Runs on Thursdays", example = "true")
  val thursday: Boolean,

  @Schema(description = "Runs on Fridays", example = "true")
  val friday: Boolean,

  @Schema(description = "Runs on Saturdays", example = "true")
  val saturday: Boolean,

  @Schema(description = "Runs on Sundays", example = "true")
  val sunday: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity Pay Rates")
data class MigrationPayRates(

  @Schema(description = "Incentive level code", example = "BAS")
  val incentiveLevelCode: String,

  @Schema(description = "Pay band", example = "1")
  val payBand: String,

  @Schema(description = "rate", example = "3.2")
  val rate: BigDecimal,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allocations")
data class MigrationAllocations(

  @Schema(description = "Nomis ID", example = "A1234BC")
  val nomisId: String,

  @Schema(description = "ID of the active booking", example = "12345")
  val bookingId: Long,

  @Schema(description = "Date allocated to the course", example = "2023-03-12")
  val startDate: LocalDate,

  @Schema(description = "Date deallocated from the course", example = "2023-05-26")
  val endDate: LocalDate?,

  @Schema(description = "Deallocation comment", example = "Removed due to schedule clash")
  val endComment: String?,

  @Schema(description = "Whether the prisoner is currently suspended from the course", example = "false")
  val suspended: Boolean,

  @Schema(description = "Suspension comments", example = "Suspended for bad behaviour")
  val suspendedComment: String?,
)

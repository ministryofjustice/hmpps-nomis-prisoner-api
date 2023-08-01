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

  private fun findPrison(prisonId: String) =
    agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw NotFoundException("Prison with id=$prisonId does not exist")
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find migration activities request")
data class FindMigrationActivitiesResponse(
  @Schema(description = "The activity id to be migrated", example = "1")
  val courseActivityId: Long,
)

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository

@Service
@Transactional(readOnly = true)
class MigrationService(
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun findMigrationActivities(prisonId: String) =
    findPrison(prisonId).let { FindMigrationActivitiesResponse(listOf()) }

  private fun findPrison(prisonId: String) =
    agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw NotFoundException("Prison with id=$prisonId does not exist")
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find migration activities request")
data class FindMigrationActivitiesResponse(
  @Schema(description = "The activities to be migrated", example = "[1, 2]")
  val activityIds: List<Long>,
)

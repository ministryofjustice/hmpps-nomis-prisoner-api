package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "activities-resource")
class MigrationResource(
  private val migrationService: MigrationService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/activities/migrate/{prisonId}")
  @Operation(
    summary = "Find activities for a prison to migrate into DPS",
    description = "Searches for active course activities with allocated prisoners. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = FindMigrationActivitiesResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_ACTIVITIES",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun findMigrationActivities(
    @PageableDefault(sort = ["courseActivityId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @Schema(description = "Prison id", required = true) @PathVariable prisonId: String,
  ): Page<FindMigrationActivitiesResponse> =
    migrationService.findMigrationActivities(pageRequest, prisonId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/activities/{courseActivityId}/migrate")
  @Operation(
    summary = "Get activity details to migrate",
    description = "Gets all details required to migrate an Activity to DPS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = GetActivityMigrationResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_ACTIVITIES",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getActivitiesMigration(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
  ) =
    migrationService.getActivityMigration(courseActivityId)
}

package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.DayOfWeek

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class VisitsConfigurationResource(private val visitsConfigurationService: VisitsConfigurationService) {
  @GetMapping("/visits/configuration/time-slots/ids")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all visit time slot Ids",
    description = "Retrieves all visit time slot ids - typically for a migration. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of time slots Ids",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getVisitTimeSlotIds(
    @PageableDefault(size = 20, sort = ["createDatetime"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
  ): Page<VisitTimeSlotIdResponse> = visitsConfigurationService.getVisitTimeSlotIds(pageRequest = pageRequest)
}

data class VisitTimeSlotIdResponse(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
  @Schema(description = "Day of the week time slot is for", example = "MONDAY")
  val dayOfWeek: DayOfWeek,
  @Schema(description = "The time slot sequence", example = "1")
  val timeSlotSequence: Long,
)

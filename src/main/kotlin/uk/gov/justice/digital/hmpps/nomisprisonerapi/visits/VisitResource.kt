package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDateTime

const val OFFENDER_NO_PATTERN = "[A-Z]\\d{4}[A-Z]{2}"

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitResource(private val visitService: VisitService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @PostMapping("/prisoners/{offenderNo}/visits")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new visit",
    description = "Creates a new visit and decrements the visit balance.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateVisitRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit information with created id",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison or person ids do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        responseCode = "404",
        description = "offenderNo does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "visit already exists exist. The moreInfo contains the NOMIS visitId for the existing visit",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createVisit(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @RequestBody @Valid
    createVisitRequest: CreateVisitRequest,
  ): CreateVisitResponse =
    visitService.createVisit(offenderNo, createVisitRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @PutMapping("/prisoners/{offenderNo}/visits/{visitId}")
  @Operation(
    summary = "Updates an existing visit",
    description = "Updates details of an existing visit such as the visitors and time slot",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateVisitRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit information updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Person ids do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        responseCode = "404",
        description = "offenderNo or visits id does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateVisit(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @Schema(description = "Nomis visit Id", example = "123456", required = true)
    @PathVariable
    visitId: Long,
    @RequestBody @Valid
    updateVisitRequest: UpdateVisitRequest,
  ): Unit =
    visitService.updateVisit(offenderNo, visitId, updateVisitRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @PutMapping("/prisoners/{offenderNo}/visits/{visitId}/cancel")
  @Operation(
    summary = "Cancel a visit",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit cancelled",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid cancellation reason",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "VSIP visit id not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
    ],
  )
  fun cancelVisit(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @Schema(description = "Nomis Visit Id", required = true)
    @PathVariable
    visitId: Long,
    @RequestBody @Valid
    cancelVisitRequest: CancelVisitRequest,
  ) {
    visitService.cancelVisit(offenderNo, visitId, cancelVisitRequest)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/visits/{visitId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get visit",
    description = "Retrieves a visit by id.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitResponse::class))],
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
        responseCode = "404",
        description = "visit does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getVisit(
    @Schema(description = "Nomis Visit Id", example = "12345", required = true)
    @PathVariable
    visitId: Long,
  ): VisitResponse =
    visitService.getVisit(visitId)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/visits/ids")
  @Operation(
    summary = "get visits by filter",
    description = "Retrieves a paged list of visits by filter",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of visit ids is returned",
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
    ],
  )
  fun getVisitsByFilter(
    @PageableDefault(sort = ["whenCreated"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(
      description = "Filter results by prison ids (returns all prisons if not specified)",
      example = "['MDI','LEI']",
    )
    prisonIds: List<String>?,
    @RequestParam(value = "visitTypes", required = false)
    @Parameter(
      description = "Filter results by visitType (returns all types if not specified)",
      example = "['SCON','OFFI']",
    )
    visitTypes: List<String>?,
    @RequestParam(value = "fromDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that were created on or after the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    fromDateTime: LocalDateTime?,
    @RequestParam(value = "toDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that were created on or before the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    toDateTime: LocalDateTime?,
  ): Page<VisitIdResponse> =
    visitService.findVisitIdsByFilter(
      pageRequest = pageRequest,
      VisitFilter(
        visitTypes = visitTypes ?: listOf(),
        prisonIds = prisonIds ?: listOf(),
        toDateTime = toDateTime,
        fromDateTime = fromDateTime,
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/visits/rooms/usage-count")
  @Operation(
    summary = "get future visit room usage by filter",
    description = "Retrieves a list of rooms with usage count for the (filtered) visits. Only future visits are included",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "list of visit room and count is returned",
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
    ],
  )
  fun getVisitRoomCountsByFilter(
    @PageableDefault(sort = ["whenCreated"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(
      description = "Filter results by prison ids (returns all prisons if not specified)",
      example = "['MDI','LEI']",
    )
    prisonIds: List<String>?,
    @RequestParam(value = "visitTypes", required = false)
    @Parameter(
      description = "Filter results by visitType (returns all types if not specified)",
      example = "['SCON','OFFI']",
    )
    visitTypes: List<String>?,
    @RequestParam(value = "fromDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that were created on or after the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    fromDateTime: LocalDateTime?,
    @RequestParam(value = "toDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that were created on or before the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    toDateTime: LocalDateTime?,
    @RequestParam(value = "futureVisitsOnly", required = false)
    @Parameter(
      description = "Filter results by restricting to future visit usage only",
      example = "true",
    )
    futureVisitsOnly: Boolean?,
  ): List<VisitRoomCountResponse> =
    visitService.findRoomCountsByFilter(
      VisitFilter(
        visitTypes = visitTypes ?: listOf(),
        prisonIds = prisonIds ?: listOf(),
        toDateTime = toDateTime,
        fromDateTime = fromDateTime,
        futureVisits = futureVisitsOnly ?: true,
        // apply filtering of bad data if only restricting usage to future dates
        excludeExtremeFutureDates = futureVisitsOnly ?: true,
      ),
    )
}

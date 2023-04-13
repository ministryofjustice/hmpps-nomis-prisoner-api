package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAttendanceStatusRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAttendanceStatusResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.SchedulesRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivitiesResource(
  private val activityService: ActivityService,
  private val allocationService: AllocationService,
  private val attendanceService: AttendanceService,
) {
  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/activities")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new activity",
    description = "Creates a new activity and associated pay rates. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Activity information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison, location, program service or iep value do not exist",
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
    ],
  )
  fun createActivity(
    @RequestBody @Valid
    createActivityRequest: CreateActivityRequest,
  ): CreateActivityResponse =
    activityService.createActivity(createActivityRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PutMapping("/activities/{courseActivityId}")
  @Operation(
    summary = "Updates an activity",
    description = "Updates an activity and associated pay rates. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = UpdateActivityRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Activity information",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison, location, program service or iep value do not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
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
        description = "Activity Not Found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun updateActivity(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    updateActivityRequest: UpdateActivityRequest,
  ) = activityService.updateActivity(courseActivityId, updateActivityRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/activities/{courseActivityId}/allocations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Allocates a prisoner to an activity",
    description = "Allocates a prisoner to an activity. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateAllocationRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Offender program profile information with created id",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateAllocationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = """One or more of the following is true:<ul>
        <li>the booking id does not exist,</li>
        <li>the prisoner is already allocated,</li>
        <li>the course is held at a different prison to the prisoner's location,</li>
        <li>the pay band code does not exist for the given course activity.</li></ul>
        """,
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
        description = "The course activity does not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun createOffenderProgramProfile(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    createRequest: CreateAllocationRequest,
  ): CreateAllocationResponse =
    allocationService.createAllocation(courseActivityId, createRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PutMapping("/activities/{courseActivityId}/allocations")
  @Operation(
    summary = "Updates a prisoner's allocation to an activity",
    description = "Updates a prisoner's allocation to an activity. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateAllocationRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Success",
      ),
      ApiResponse(
        responseCode = "400",
        description = """One or more of the following is true:<ul>
        <li>the prisoner is not allocated to the course,</li>
        <li>the course or prisoner does not exist,</li>
        <li>the end date is missing or invalid,</li>
        <li>the reason is invalid</li>
        </ul>
        """,
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
        description = "The course activity or booking id do not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun endOffenderProgramProfile(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    updateRequest: UpdateAllocationRequest,
  ) =
    allocationService.updateAllocation(courseActivityId, updateRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PutMapping("/activities/{courseActivityId}/schedules")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates activity schedules",
    description = "Recreates schedules from tomorrow. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Schedules updated",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "There was an error with the request",
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
    ],
  )
  fun updateSchedules(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    scheduleRequests: List<SchedulesRequest>,
  ) = activityService.updateActivitySchedules(courseActivityId, scheduleRequests)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/activities/{courseActivityId}/booking/{bookingId}/attendance")
  @Operation(
    summary = "Creates or updates an attendance record",
    description = "Creates or updates an attendance for the booking and schedule. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance updated",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateActivityResponse::class)),
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
    ],
  )
  fun upsertAttendance(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @Schema(description = "Booking id", required = true) @PathVariable bookingId: Long,
    @RequestBody @Valid
    upsertAttendanceRequest: UpsertAttendanceRequest,
  ) =
    attendanceService.upsertAttendance(courseActivityId, bookingId, upsertAttendanceRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/activities/{courseActivityId}/booking/{bookingId}/attendance-status")
  @Operation(
    summary = "Get Nomis attendance status",
    description = "Returns the current event status of a Nomis attendance record. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance status found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = GetAttendanceStatusResponse::class)),
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
        description = "The attendance record does not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getAttendanceStatus(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @Schema(description = "Booking id", required = true) @PathVariable bookingId: Long,
    @RequestBody @Valid
    request: GetAttendanceStatusRequest,
  ) =
    attendanceService.findAttendanceStatus(courseActivityId, bookingId, request)

  @Hidden
  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @DeleteMapping("/activities/{courseActivityId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteActivity(@PathVariable courseActivityId: Long) = activityService.deleteActivity(courseActivityId)
}

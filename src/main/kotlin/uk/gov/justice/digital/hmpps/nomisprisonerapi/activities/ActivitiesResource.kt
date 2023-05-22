package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CourseScheduleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAttendanceStatusRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAttendanceStatusResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateCourseScheduleResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivitiesResource(
  private val activityService: ActivityService,
  private val allocationService: AllocationService,
  private val attendanceService: AttendanceService,
  private val scheduleService: ScheduleService,
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
  @PutMapping("/activities/{courseActivityId}/allocation")
  @Operation(
    summary = "Creates or Updates a prisoner's allocation to an activity",
    description = "Creates or updates a prisoner's allocation to an activity. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpsertAllocationRequest::class),
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
        description = "There was an error with the request, see the response for details",
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
  fun upsertAllocation(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    upsertRequest: UpsertAllocationRequest,
  ) =
    allocationService.upsertAllocation(courseActivityId, upsertRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PutMapping("/activities/{courseActivityId}/schedule")
  @Operation(
    summary = "Updates a course schedule",
    description = "Updates a course schedule. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourseScheduleRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Success",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = UpdateCourseScheduleResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
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
        description = "The course schedule does not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun updateCourseSchedule(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
    @RequestBody @Valid
    updateRequest: CourseScheduleRequest,
  ) =
    scheduleService.updateCourseSchedule(courseActivityId, updateRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/activities/{courseActivityId}/booking/{bookingId}/attendance")
  @Operation(
    summary = "Creates or updates an attendance record",
    description = "Creates or updates an attendance for the booking and schedule. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = UpsertAttendanceRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance updated",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = UpsertAttendanceResponse::class)),
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
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = GetAttendanceStatusRequest::class)),
      ],
    ),
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

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @DeleteMapping("/activities/{courseActivityId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a NOMIS course activity",
    description = "Deletes a course activity and its children - pay rates, schedules, allocations and attendances. Intended to be used for data fixes. Requires role NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = GetAttendanceStatusRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Activity is deleted",
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
    ],
  )
  fun deleteActivity(@PathVariable courseActivityId: Long) = activityService.deleteActivity(courseActivityId)
}

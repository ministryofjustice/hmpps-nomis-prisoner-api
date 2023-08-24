package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CourseScheduleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.FindActiveActivityIdsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.FindActiveAllocationIdsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.GetAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateCourseScheduleResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate

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
  ): CreateActivityResponse = activityService.updateActivity(courseActivityId, updateActivityRequest)

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
  @PutMapping("/schedules/{courseScheduleId}/booking/{bookingId}/attendance")
  @Operation(
    summary = "Creates or updates an attendance record",
    description = "Creates or updates an attendance for the course schedule. Requires role NOMIS_ACTIVITIES",
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
    @Schema(description = "Course schedule id", required = true) @PathVariable courseScheduleId: Long,
    @Schema(description = "Booking id", required = true) @PathVariable bookingId: Long,
    @RequestBody @Valid
    upsertAttendanceRequest: UpsertAttendanceRequest,
  ) =
    attendanceService.upsertAttendance(courseScheduleId, bookingId, upsertAttendanceRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/activities/ids")
  @Operation(
    summary = "Find paged active activities",
    description = "Searches for active course activities with allocated prisoners. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = FindActiveActivityIdsResponse::class)),
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
  fun findActiveActivities(
    @PageableDefault(sort = ["courseActivityId"], direction = Sort.Direction.ASC) pageRequest: Pageable,
    @Schema(description = "Prison id", required = true) @RequestParam prisonId: String,
    @Schema(description = "Exclude program codes", name = "excludeProgramCode", required = false)
    @RequestParam(name = "excludeProgramCode")
    excludeProgramCodes: List<String>?,
    @Schema(description = "Course Activity ID", required = false) @RequestParam courseActivityId: Long?,
  ): Page<FindActiveActivityIdsResponse> =
    activityService.findActiveActivityIds(pageRequest, prisonId, excludeProgramCodes, courseActivityId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/activities/{courseActivityId}")
  @Operation(
    summary = "Get activity details",
    description = "Gets activity details including schedule rules and pay rates. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = GetActivityResponse::class)),
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
  fun getActivity(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
  ) =
    activityService.getActivity(courseActivityId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/allocations/ids")
  @Operation(
    summary = "Find paged active allocations",
    description = "Searches for active course allocations. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = FindActiveAllocationIdsResponse::class)),
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
  fun findActiveAllocations(
    @PageableDefault(sort = ["offenderProgramReferenceId"], direction = Sort.Direction.ASC) pageRequest: Pageable,
    @Schema(description = "Prison id", required = true) @RequestParam prisonId: String,
    @Schema(description = "Exclude program codes", name = "excludeProgramCode", required = false)
    @RequestParam(name = "excludeProgramCode")
    excludeProgramCodes: List<String>?,
    @Schema(description = "Course Activity ID", required = false) @RequestParam courseActivityId: Long?,
  ): Page<FindActiveAllocationIdsResponse> =
    allocationService.findActiveAllocations(pageRequest, prisonId, excludeProgramCodes, courseActivityId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/allocations/{allocationId}")
  @Operation(
    summary = "Get allocation details",
    description = "Gets allocation details. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = GetAllocationResponse::class)),
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
  fun getAllocation(
    @Schema(description = "Allocation id", required = true) @PathVariable allocationId: Long,
  ) =
    allocationService.getAllocation(allocationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @DeleteMapping("/activities/{courseActivityId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a NOMIS course activity",
    description = "Deletes a course activity and its children - pay rates, schedules, allocations and attendances. Intended to be used for data fixes. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Activity is deleted",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @DeleteMapping("/allocations/{referenceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a NOMIS allocation (from OFFENDER_PROGRAM_PROFILES table)",
    description = "Deletes an allocation from NOMIS and any children - pay rates, attendances. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Allocation is deleted",
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
  fun deleteAllocation(@PathVariable referenceId: Long) = allocationService.deleteAllocation(referenceId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @DeleteMapping("/attendances/{eventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a NOMIS attendance (from OFFENDER_COURSE_ATTENDANCES table)",
    description = "Deletes an attendance from NOMIS. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Attendance is deleted",
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
  fun deleteAttendance(@PathVariable eventId: Long) = attendanceService.deleteAttendance(eventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PutMapping("/activities/{courseActivityId}/end")
  @Operation(
    summary = "End a course activity",
    description = "Ends a course activity and all active attendances with end date today. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Activity ended",
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
  fun endActivity(
    @Schema(description = "Course activity id", required = true) @PathVariable courseActivityId: Long,
  ) =
    activityService.endActivity(courseActivityId, LocalDate.now())
}

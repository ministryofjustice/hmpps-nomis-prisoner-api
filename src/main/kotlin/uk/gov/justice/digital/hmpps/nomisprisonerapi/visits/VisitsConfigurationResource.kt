package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalDate
import java.time.LocalTime

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
    @PageableDefault(size = 20, sort = ["createDatetime", "agencyVisitTimesId.location", "agencyVisitTimesId.weekDay", "agencyVisitTimesId.timeSlotSequence"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
  ): PagedModel<VisitTimeSlotIdResponse> = PagedModel(visitsConfigurationService.getVisitTimeSlotIds(pageRequest = pageRequest))

  @GetMapping("/visits/configuration/time-slots/prison-id/{prisonId}/day-of-week/{dayOfWeek}/time-slot-sequence/{timeSlotSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get visit time slot",
    description = "Retrieves visit time slot along with visit slots. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Time slot with visits slots",
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
      ApiResponse(
        responseCode = "404",
        description = "Visit time slot not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getVisitTimeSlot(
    @PathVariable prisonId: String,
    @PathVariable dayOfWeek: WeekDay,
    @PathVariable timeSlotSequence: Int,
  ): VisitTimeSlotResponse = visitsConfigurationService.getVisitTimeSlot(prisonId = prisonId, dayOfWeek = dayOfWeek, timeSlotSequence = timeSlotSequence)

  @GetMapping("/visits/configuration/prisons")
  @Operation(
    summary = "Get a list of prisonIds that are active have timeslots configured",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "List of prisons",
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
  fun getActivePrisonsWithTimeSlots(): ActivePrisonWithTimeSlotResponse = ActivePrisonWithTimeSlotResponse(visitsConfigurationService.getActivePrisonsWithTimeSlots())

  @GetMapping("/visits/configuration/time-slots/prison-id/{prisonId}")
  @Operation(
    summary = "Get a list of timeslots for specific prison",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "List of timeslots",
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
  fun getPrisonVisitTimeSlots(
    @PathVariable
    prisonId: String,
    @Schema(description = "If supplied only return time slots that have not expired", required = false, example = "true")
    @RequestParam(value = "activeOnly", defaultValue = "true")
    activeOnly: Boolean,
  ): VisitTimeSlotForPrisonResponse = visitsConfigurationService.getPrisonVisitTimeSlots(
    prisonId = prisonId,
    activeOnly = activeOnly,
  )
}

data class VisitTimeSlotIdResponse(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
  @Schema(description = "Day of the week time slot is for", example = "MON")
  val dayOfWeek: WeekDay,
  @Schema(description = "The time slot sequence", example = "1")
  val timeSlotSequence: Int,
)

data class VisitTimeSlotForPrisonResponse(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
  @Schema(description = "The time slots for the prison")
  val timeSlots: List<VisitTimeSlotResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitTimeSlotResponse(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
  @Schema(description = "Day of the week time slot is for", example = "MON")
  val dayOfWeek: WeekDay,
  @Schema(description = "The time slot sequence", example = "1")
  val timeSlotSequence: Int,
  @Schema(description = "Slot start time", example = "10:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,
  @Schema(description = "Slot end time", example = "11:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,
  @Schema(description = "Date slot can first be used", example = "2022-09-01")
  val effectiveDate: LocalDate,
  @Schema(description = "Date slot can no longer be used", example = "2032-09-01")
  val expiryDate: LocalDate?,
  @Schema(description = "List of slots at this time slot")
  val visitSlots: List<VisitSlotResponse>,
  @Schema(description = "Audit information")
  val audit: NomisAudit,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitSlotResponse(
  @Schema(description = "Slot ID", example = "1")
  val id: Long,
  @Schema(description = "Room location of  visit slot")
  val internalLocation: VisitInternalLocationResponse,
  @Schema(description = "Optional max groups allowed in slot", example = "1")
  val maxGroups: Int?,
  @Schema(description = "Optional max adults allowed in slot", example = "1")
  val maxAdults: Int?,
  @Schema(description = "Audit information")
  val audit: NomisAudit,
)

data class VisitInternalLocationResponse(
  val id: Long,
  val code: String,
)

data class ActivePrison(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
)
data class ActivePrisonWithTimeSlotResponse(
  val prisons: List<ActivePrison>,
)
